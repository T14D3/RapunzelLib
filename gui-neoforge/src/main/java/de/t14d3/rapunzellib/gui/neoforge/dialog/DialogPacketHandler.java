package de.t14d3.rapunzellib.gui.neoforge.dialog;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.t14d3.rapunzellib.gui.shared.dialog.SharedDialogPayload;
import de.t14d3.rapunzellib.gui.shared.dialog.SharedDialogSubmission;
import de.t14d3.rapunzellib.gui.shared.dialog.SharedDialogSessions;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NeoForge packet transport for shared dialogs.
 * <p>
 * The dialog is serialized to JSON and shipped to the vanilla client as a custom
 * {@link CustomPacketPayload} on the play channel (clientbound {@link DialogOpenPayload}).
 * The client's dialog screen responds with a serverbound {@link DialogResponsePayload}
 * carrying the JSON-encoded field values, which are parsed back into a
 * {@link SharedDialogSubmission} and handed to {@link SharedDialogSessions}.
 * <p>
 * Registration happens through {@link #registerPayloadHandlers(RegisterPayloadHandlersEvent)},
 * which is wired to the mod event bus by {@code NeoForgeGuiMod}. No lazy initialization is
 * needed because NeoForge registers payloads declaratively at mod construction time.
 */
public final class DialogPacketHandler {

    private static final Gson GSON = new Gson();

    private DialogPacketHandler() {
    }

    /**
     * Registers the dialog payloads with the NeoForge payload registrar.
     *
     * @param event the payload handler registration event
     */
    public static void registerPayloadHandlers(@NotNull RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("rapunzellib_gui_neoforge");
        registrar.playToClient(DialogOpenPayload.TYPE, DialogOpenPayload.CODEC);
        registrar.playToServer(DialogResponsePayload.TYPE, DialogResponsePayload.CODEC, DialogPacketHandler::handleServerbound);
    }

    /**
     * Sends a dialog payload to the given player.
     *
     * @param player        the recipient
     * @param dialogPayload the dialog payload to send
     * @return {@code true} if the payload was sent
     */
    public static boolean sendDialog(
        @NotNull ServerPlayer player,
        @NotNull SharedDialogPayload dialogPayload
    ) {
        if (!canSendDialog(player)) {
            return false;
        }

        PacketDistributor.sendToPlayer(player, new DialogOpenPayload(serializePayload(dialogPayload)));
        return true;
    }

    /**
     * Checks whether a dialog payload can be sent to the given player.
     * <p>
     * NeoForge does not expose a per-player capability check like Fabric's
     * {@code ServerPlayNetworking.canSend}; players on modern Minecraft always
     * negotiate custom payloads as part of the play handshake. The only meaningful
     * guard is that the player is still connected.
     *
     * @param player the recipient
     * @return {@code true} if the player can receive dialog payloads
     */
    public static boolean canSendDialog(@NotNull ServerPlayer player) {
        return !player.isRemoved();
    }

    /**
     * Submits dialog responses for a player.
     *
     * @param playerId  the player UUID
     * @param responses the parsed responses
     */
    public static void handleResponse(@NotNull UUID playerId, @NotNull SharedDialogSubmission responses) {
        SharedDialogSessions.submit(playerId, responses);
    }

    /**
     * Handles a serverbound dialog response payload, parsing it and submitting the
     * responses on the server thread.
     *
     * @param payload the response payload
     * @param context the network context
     */
    private static void handleServerbound(DialogResponsePayload payload, IPayloadContext context) {
        if (payload == null || payload.json() == null) {
            return;
        }
        if (context == null) {
            return;
        }
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(() -> {
            SharedDialogSubmission responses = parseResponses(payload.json());
            handleResponse(player.getUUID(), responses);
        });
    }

    private static @NotNull String serializePayload(@NotNull SharedDialogPayload payload) {
        JsonObject root = new JsonObject();
        root.addProperty("title", payload.title());

        JsonArray bodies = new JsonArray();
        for (SharedDialogPayload.Body body : payload.bodies()) {
            bodies.add(serializeBody(body));
        }
        root.add("bodies", bodies);

        JsonArray inputs = new JsonArray();
        for (SharedDialogPayload.Input input : payload.inputs()) {
            inputs.add(serializeInput(input));
        }
        root.add("inputs", inputs);

        return GSON.toJson(root);
    }

    private static @NotNull JsonObject serializeBody(@NotNull SharedDialogPayload.Body body) {
        JsonObject entry = new JsonObject();
        switch (body) {
            case SharedDialogPayload.ButtonBody button -> {
                entry.addProperty("type", "button");
                entry.addProperty("label", button.label());
                entry.addProperty("enabled", button.enabled());
                if (button.tooltip() != null) {
                    entry.addProperty("tooltip", Arrays.toString(button.tooltip()));
                }
                if (button.icon() != null) {
                    entry.addProperty("icon", button.icon());
                }
            }
            case SharedDialogPayload.TextBody text -> {
                entry.addProperty("type", "text");
                entry.addProperty("text", text.text());
            }
            case SharedDialogPayload.DividerBody ignored -> entry.addProperty("type", "divider");
            case SharedDialogPayload.SpacerBody spacer -> {
                entry.addProperty("type", "spacer");
                entry.addProperty("height", spacer.height());
            }
        }
        return entry;
    }

    private static @NotNull JsonObject serializeInput(@NotNull SharedDialogPayload.Input input) {
        JsonObject entry = new JsonObject();
        entry.addProperty("key", input.key());
        entry.addProperty("label", input.label());
        switch (input) {
            case SharedDialogPayload.TextInput text -> {
                entry.addProperty("type", "text");
                entry.addProperty("placeholder", text.placeholder());
                entry.add("default", text.defaultValue() != null ? GSON.toJsonTree(text.defaultValue()) : JsonNull.INSTANCE);
                entry.addProperty("maxLength", text.maxLength());
            }
            case SharedDialogPayload.ToggleInput toggle -> {
                entry.addProperty("type", "toggle");
                entry.addProperty("default", toggle.defaultValue());
            }
            case SharedDialogPayload.SliderInput slider -> {
                entry.addProperty("type", "slider");
                entry.addProperty("min", slider.min());
                entry.addProperty("max", slider.max());
                entry.addProperty("step", slider.step());
                entry.addProperty("default", slider.defaultValue());
            }
            case SharedDialogPayload.DropdownInput dropdown -> {
                entry.addProperty("type", "dropdown");
                JsonArray options = new JsonArray();
                for (SharedDialogPayload.DropdownOption option : dropdown.options()) {
                    JsonObject optionEntry = new JsonObject();
                    optionEntry.addProperty("value", option.value());
                    optionEntry.addProperty("label", option.label());
                    options.add(optionEntry);
                }
                entry.add("options", options);
                if (dropdown.defaultValue() != null) {
                    entry.addProperty("default", dropdown.defaultValue());
                }
            }
        }
        return entry;
    }

    private static @NotNull SharedDialogSubmission parseResponses(@NotNull String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return SharedDialogSubmission.empty();
            }

            Map<String, GuiValue> responses = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                GuiValue value = parseValue(entry.getValue());
                if (value != null) {
                    responses.put(entry.getKey(), value);
                }
            }
            return new SharedDialogSubmission(responses);
        } catch (Exception ignored) {
            return SharedDialogSubmission.empty();
        }
    }

    private static @Nullable GuiValue parseValue(@NotNull JsonElement value) {
        if (!value.isJsonPrimitive()) {
            return null;
        }

        var primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return GuiValue.of(primitive.getAsBoolean());
        }
        if (primitive.isNumber()) {
            return GuiValue.of(primitive.getAsDouble());
        }
        if (primitive.isString()) {
            return GuiValue.of(primitive.getAsString());
        }
        return null;
    }

    /**
     * Clears any pending dialog session for the given player.
     *
     * @param playerId the player UUID
     */
    public static void clearPending(@NotNull UUID playerId) {
        SharedDialogSessions.clear(playerId);
    }

    /**
     * Clientbound payload carrying the JSON-encoded dialog definition.
     *
     * @param json the serialized dialog
     */
    public record DialogOpenPayload(String json) implements CustomPacketPayload {
        // #if VERSION >= 1.21.11
        public static final Identifier CHANNEL = Identifier.parse("rapunzellib:dialog_open");
        // #else
        public static final ResourceLocation CHANNEL = ResourceLocation.parse("rapunzellib:dialog_open");
        // #endif
        public static final CustomPacketPayload.Type<DialogOpenPayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogOpenPayload> CODEC = StreamCodec.of(
            DialogOpenPayload::write,
            DialogOpenPayload::read
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static DialogOpenPayload read(RegistryFriendlyByteBuf buf) {
            return new DialogOpenPayload(buf.readUtf());
        }

        private static void write(RegistryFriendlyByteBuf buf, DialogOpenPayload payload) {
            buf.writeUtf(payload.json());
        }
    }

    /**
     * Serverbound payload carrying the JSON-encoded dialog responses.
     *
     * @param json the serialized responses
     */
    public record DialogResponsePayload(String json) implements CustomPacketPayload {
        // #if VERSION >= 1.21.11
        public static final Identifier CHANNEL = Identifier.parse("rapunzellib:dialog_response");
        // #else
        public static final ResourceLocation CHANNEL = ResourceLocation.parse("rapunzellib:dialog_response");
        // #endif
        public static final CustomPacketPayload.Type<DialogResponsePayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogResponsePayload> CODEC = StreamCodec.of(
            DialogResponsePayload::write,
            DialogResponsePayload::read
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static DialogResponsePayload read(RegistryFriendlyByteBuf buf) {
            return new DialogResponsePayload(buf.readUtf());
        }

        private static void write(RegistryFriendlyByteBuf buf, DialogResponsePayload payload) {
            buf.writeUtf(payload.json());
        }
    }
}
