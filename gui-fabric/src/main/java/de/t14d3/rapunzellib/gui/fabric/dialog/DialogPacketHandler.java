package de.t14d3.rapunzellib.gui.fabric.dialog;

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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class DialogPacketHandler {

    private static final Gson GSON = new Gson();
    private static final Object INIT_LOCK = new Object();
    private static volatile boolean initialized;

    private DialogPacketHandler() {
    }

    public static void ensureInitialized() {
        if (initialized) {
            return;
        }

        synchronized (INIT_LOCK) {
            if (initialized) {
                return;
            }
            // #if VERSION >= 26.0.0
            PayloadTypeRegistry.clientboundPlay().register(DialogOpenPayload.TYPE, DialogOpenPayload.CODEC);
            PayloadTypeRegistry.serverboundPlay().register(DialogResponsePayload.TYPE, DialogResponsePayload.CODEC);
            // #else
            // # PayloadTypeRegistry.playS2C().register(DialogOpenPayload.TYPE, DialogOpenPayload.CODEC);
            // # PayloadTypeRegistry.playC2S().register(DialogResponsePayload.TYPE, DialogResponsePayload.CODEC);
            // #endif
            ServerPlayNetworking.registerGlobalReceiver(DialogResponsePayload.TYPE, DialogPacketHandler::handleResponsePacket);

            initialized = true;
        }
    }

    public static boolean sendDialog(
        @NotNull ServerPlayer player,
        @NotNull SharedDialogPayload dialogPayload
    ) {
        if (!canSendDialog(player)) {
            return false;
        }

        ServerPlayNetworking.send(player, new DialogOpenPayload(serializePayload(dialogPayload)));
        return true;
    }

    public static boolean canSendDialog(@NotNull ServerPlayer player) {
        ensureInitialized();
        return ServerPlayNetworking.canSend(player, DialogOpenPayload.CHANNEL);
    }

    public static void handleResponse(@NotNull UUID playerId, @NotNull SharedDialogSubmission responses) {
        SharedDialogSessions.submit(playerId, responses);
    }

    private static void handleResponsePacket(DialogResponsePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            SharedDialogSubmission responses = parseResponses(payload.json());
            handleResponse(context.player().getUUID(), responses);
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
                    entry.addProperty("tooltip", button.tooltip());
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

    public static void clearPending(@NotNull UUID playerId) {
        SharedDialogSessions.clear(playerId);
    }

    public record DialogOpenPayload(String json) implements CustomPacketPayload {
        // #if VERSION >= 1.21.11
        public static final Identifier CHANNEL = Identifier.parse("rapunzellib:dialog_open");
        // #else
        public static final ResourceLocation CHANNEL = ResourceLocation.parse("rapunzellib:dialog_open");
        // #endif
        public static final CustomPacketPayload.Type<DialogOpenPayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
        public static final StreamCodec<FriendlyByteBuf, DialogOpenPayload> CODEC = StreamCodec.of(
            DialogOpenPayload::write,
            DialogOpenPayload::read
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static DialogOpenPayload read(FriendlyByteBuf buf) {
            return new DialogOpenPayload(buf.readUtf());
        }

        private static void write(FriendlyByteBuf buf, DialogOpenPayload payload) {
            buf.writeUtf(payload.json());
        }
    }

    public record DialogResponsePayload(String json) implements CustomPacketPayload {
        // #if VERSION >= 1.21.11
        public static final Identifier CHANNEL = Identifier.parse("rapunzellib:dialog_response");
        // #else
        public static final ResourceLocation CHANNEL = ResourceLocation.parse("rapunzellib:dialog_response");
        // #endif
        public static final CustomPacketPayload.Type<DialogResponsePayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
        public static final StreamCodec<FriendlyByteBuf, DialogResponsePayload> CODEC = StreamCodec.of(
            DialogResponsePayload::write,
            DialogResponsePayload::read
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static DialogResponsePayload read(FriendlyByteBuf buf) {
            return new DialogResponsePayload(buf.readUtf());
        }

        private static void write(FriendlyByteBuf buf, DialogResponsePayload payload) {
            buf.writeUtf(payload.json());
        }
    }
}
