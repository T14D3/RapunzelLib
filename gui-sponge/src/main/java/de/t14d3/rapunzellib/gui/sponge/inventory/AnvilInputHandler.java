package de.t14d3.rapunzellib.gui.sponge.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryEventBridge;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiInteractionEngine;
import de.t14d3.rapunzellib.gui.core.GuiSessionStore;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import java.util.*;

public final class AnvilInputHandler {

    private final InventoryRenderer renderer;
    private final GuiSessionStore<ActiveAnvilInput> activeInputs = new GuiSessionStore<>();

    AnvilInputHandler(InventoryRenderer renderer) {
        this.renderer = renderer;
    }

    void openAnvilInput(@NotNull ServerPlayer player, @NotNull InputElement input, @NotNull RenderContext context) {
        closeAnvilInput(player.uniqueId());

        ViewableInventory inventory = ViewableInventory.builder()
            .type(ContainerTypes.ANVIL)
            .completeStructure()
            .build();
        RInventory wrappedInventory = wrapInventory(inventory);

        Component label = input.label() != null ? input.label() : Component.text("Enter text");

        String currentValue = context.get(input.key(), String.class, input.defaultValue());
        Component inputName = (currentValue != null && !currentValue.isEmpty())
            ? Component.text(currentValue) : label;

        ItemStack inputItem = ItemStack.builder()
            .itemType(ItemTypes.PAPER.get())
            .quantity(1)
            .add(Keys.CUSTOM_NAME, inputName)
            .build();

        Optional<Slot> inputSlot = inventory.slot(0);
        final ItemStack finalInputItem = inputItem;
        inputSlot.ifPresent(slot -> slot.set(finalInputItem));

        InventoryMenu menu = InventoryMenu.of(inventory);
        menu.setReadOnly(true);

        menu.registerSlotClick((cause, container, slot, slotIndex, clickType) -> {
            ServerPlayer clickPlayer = cause.first(ServerPlayer.class).orElse(null);
            if (clickPlayer != null) {
                var eventClickType = SpongeGuiClickTypes.mapInventoryClick(clickType);
                InventoryEventBridge.ClickDispatch clickDispatch = InventoryEventBridge.dispatchClick(
                    context.player(),
                    wrappedInventory,
                    slotIndex,
                    eventClickType
                );
                try {
                    if (!clickDispatch.cancelled() && slotIndex == 2) {
                        Optional<Slot> outputSlot = inventory.slot(2);
                        if (outputSlot.isPresent()) {
                            ItemStack result = outputSlot.get().peek();
                            if (!result.isEmpty()) {
                                Optional<Component> nameOpt = result.get(Keys.CUSTOM_NAME);
                                if (nameOpt.isPresent()) {
                                    String text = PlainTextComponentSerializer.plainText().serialize(nameOpt.get());
                                    handleInputSubmission(clickPlayer, input, context, text);
                                }
                            }
                        }
                    }
                } finally {
                    clickDispatch.post();
                }
            }
            return false;
        });

        menu.registerClose((cause, container) -> {
            ServerPlayer closePlayer = cause.first(ServerPlayer.class).orElse(null);
            if (closePlayer != null) {
                InventoryEventBridge.dispatchClose(context.player(), wrappedInventory);
                activeInputs.remove(closePlayer.uniqueId());
            }
        });

        ActiveAnvilInput activeInput = new ActiveAnvilInput(input, context, inventory, menu);
        if (!InventoryEventBridge.dispatchOpenPre(context.player(), wrappedInventory)) {
            return;
        }

        activeInputs.put(player.uniqueId(), activeInput);

        menu.open(player);
        InventoryEventBridge.dispatchOpen(context.player(), wrappedInventory);
    }

    void closeAnvilInput(@NotNull UUID playerId) {
        ActiveAnvilInput active = activeInputs.remove(playerId);
        if (active != null) {
            ServerPlayer player = Sponge.server().player(playerId).orElse(null);
            if (player != null) {
                player.closeInventory();
            }
        }
    }

    @Nullable
    ActiveAnvilInput getActiveInput(@NotNull UUID playerId) {
        return activeInputs.get(playerId);
    }

    private void handleInputSubmission(@NotNull ServerPlayer player, @NotNull InputElement input,
                                         @NotNull RenderContext context, @NotNull String text) {
        GuiInteractionEngine.submitInput(input, context.state(), context.player(), text);

        closeAnvilInput(player.uniqueId());

        InventoryRenderer.ActiveGui parentGui = renderer.getActiveGui(player.uniqueId());
        if (parentGui != null) {
            renderer.render(parentGui.gui, context.player(), parentGui.context);
        }
    }

    private static @NotNull RInventory wrapInventory(@NotNull ViewableInventory inventory) {
        return InventoryFeatures.install().require(inventory);
    }

    static final class ActiveAnvilInput {
        final InputElement input;
        final RenderContext context;
        final ViewableInventory inventory;
        final InventoryMenu menu;

        ActiveAnvilInput(InputElement input, RenderContext context, ViewableInventory inventory, InventoryMenu menu) {
            this.input = input;
            this.context = context;
            this.inventory = inventory;
            this.menu = menu;
        }
    }
}
