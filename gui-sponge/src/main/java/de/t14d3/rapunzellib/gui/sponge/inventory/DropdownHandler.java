package de.t14d3.rapunzellib.gui.sponge.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryEventBridge;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiElementStates;
import de.t14d3.rapunzellib.gui.core.GuiInteractionEngine;
import de.t14d3.rapunzellib.gui.core.GuiSessionStore;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import java.util.*;

public final class DropdownHandler {

    private final InventoryRenderer renderer;
    private final GuiSessionStore<ActiveDropdown> activeDropdowns = new GuiSessionStore<>();

    DropdownHandler(InventoryRenderer renderer) {
        this.renderer = renderer;
    }

    void openDropdown(@NotNull ServerPlayer player, @NotNull DropdownElement dropdown, @NotNull RenderContext context) {
        closeDropdown(player.uniqueId());

        List<Option> options = dropdown.options();
        int optionCount = options.size();
        int rows = Math.max(1, Math.min(6, (optionCount + 8) / 9));

        ViewableInventory inventory = ViewableInventory.builder()
            .type(containerType(rows))
            .fillDummy()
            .completeStructure()
            .build();
        RInventory wrappedInventory = wrapInventory(inventory);

        String selectedId = GuiElementStates.dropdown(dropdown, context.state()).selectedId();

        int index = 0;
        for (Inventory inv : inventory.slots()) {
            if (inv instanceof Slot slot && index < optionCount) {
                Option option = options.get(index);
                boolean isSelected = option.id().equals(selectedId);
                slot.set(SpongeInventoryBuilder.renderDropdownOption(option, isSelected));
                index++;
            }
        }

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
                    if (!clickDispatch.cancelled() && slotIndex >= 0 && slotIndex < options.size()) {
                        handleOptionClick(clickPlayer, dropdown, context, options.get(slotIndex));
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
                activeDropdowns.remove(closePlayer.uniqueId());
            }
        });

        ActiveDropdown activeDropdown = new ActiveDropdown(dropdown, context, inventory, menu, options);
        if (!InventoryEventBridge.dispatchOpenPre(context.player(), wrappedInventory)) {
            return;
        }

        activeDropdowns.put(player.uniqueId(), activeDropdown);

        menu.open(player);
        InventoryEventBridge.dispatchOpen(context.player(), wrappedInventory);
    }

    void closeDropdown(@NotNull UUID playerId) {
        ActiveDropdown active = activeDropdowns.remove(playerId);
        if (active != null) {
            ServerPlayer player = Sponge.server().player(playerId).orElse(null);
            if (player != null) {
                player.closeInventory();
            }
        }
    }

    @Nullable
    ActiveDropdown getActiveDropdown(@NotNull UUID playerId) {
        return activeDropdowns.get(playerId);
    }

    private void handleOptionClick(@NotNull ServerPlayer player, @NotNull DropdownElement dropdown,
                                       @NotNull RenderContext context, @NotNull Option selectedOption) {
        GuiInteractionEngine.selectDropdown(dropdown, context.state(), context.player(), selectedOption);

        closeDropdown(player.uniqueId());

        InventoryRenderer.ActiveGui parentGui = renderer.getActiveGui(player.uniqueId());
        if (parentGui != null) {
            renderer.render(parentGui.gui, context.player(), parentGui.context);
        }
    }

    private static @NotNull RInventory wrapInventory(@NotNull ViewableInventory inventory) {
        return InventoryFeatures.install().require(inventory);
    }

    private static @NotNull org.spongepowered.api.item.inventory.ContainerType containerType(int rows) {
        return switch (rows) {
            case 1 -> ContainerTypes.GENERIC_9X1.get();
            case 2 -> ContainerTypes.GENERIC_9X2.get();
            case 3 -> ContainerTypes.GENERIC_9X3.get();
            case 4 -> ContainerTypes.GENERIC_9X4.get();
            case 5 -> ContainerTypes.GENERIC_9X5.get();
            default -> ContainerTypes.GENERIC_9X6.get();
        };
    }

    static final class ActiveDropdown {
        final DropdownElement dropdown;
        final RenderContext context;
        final ViewableInventory inventory;
        final InventoryMenu menu;
        final List<Option> options;

        ActiveDropdown(DropdownElement dropdown, RenderContext context, ViewableInventory inventory,
                       InventoryMenu menu, List<Option> options) {
            this.dropdown = dropdown;
            this.context = context;
            this.inventory = inventory;
            this.menu = menu;
            this.options = options;
        }
    }
}
