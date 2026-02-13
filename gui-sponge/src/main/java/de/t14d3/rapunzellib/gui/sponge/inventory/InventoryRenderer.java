package de.t14d3.rapunzellib.gui.sponge.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import de.t14d3.rapunzellib.events.inventory.InventoryEventBridge;
import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiCloseHooks;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.gui.context.ClickType;
import de.t14d3.rapunzellib.gui.context.CloseReason;
import de.t14d3.rapunzellib.gui.core.GuiContexts;
import de.t14d3.rapunzellib.gui.core.GuiInventoryElementHandler;
import de.t14d3.rapunzellib.gui.core.GuiSlotPlan;
import de.t14d3.rapunzellib.gui.element.*;
import de.t14d3.rapunzellib.gui.inventory.GuiInventoryClickTypes;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import java.util.*;

public final class InventoryRenderer implements GuiRenderer {

    private static final String NAME = "sponge-inventory";
    private static final Set<GuiCapability> CAPABILITIES = GuiRendererSelectionSupport.inventoryCapabilities();

    private final Map<UUID, ActiveGui> activeGuis = new HashMap<>();
    private final AnvilInputHandler anvilInputHandler;
    private final DropdownHandler dropdownHandler;

    public InventoryRenderer() {
        this.anvilInputHandler = new AnvilInputHandler(this);
        this.dropdownHandler = new DropdownHandler(this);
    }

    @Override
    public @NotNull String name() {
        return NAME;
    }

    @Override
    public @NotNull Set<GuiCapability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public boolean supports(@NotNull GuiCapability capability) {
        return CAPABILITIES.contains(capability);
    }

    @Override
    public void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
        ServerPlayer spongePlayer = resolvePlayer(player);
        if (spongePlayer == null) {
            return;
        }

        close(gui, player);

        SpongeInventoryBuilder builder = new SpongeInventoryBuilder(this);
        ViewableInventory inventory = builder.build(gui, GuiSlotPlan.resolve(gui, 6), context);
        RInventory wrappedInventory = wrapInventory(inventory);

        InventoryMenu menu = InventoryMenu.of(inventory);
        menu.setReadOnly(true);

        menu.registerSlotClick((cause, container, slot, slotIndex, clickType) -> {
            ServerPlayer clickPlayer = cause.first(ServerPlayer.class).orElse(null);
            if (clickPlayer != null) {
                InventoryClickType eventClickType = SpongeGuiClickTypes.mapInventoryClick(clickType);
                ClickType mappedClickType = GuiInventoryClickTypes.fromEventClickType(eventClickType);
                InventoryEventBridge.ClickDispatch clickDispatch = InventoryEventBridge.dispatchClick(
                    context.player(),
                    wrappedInventory,
                    slotIndex,
                    eventClickType
                );
                try {
                    if (!clickDispatch.cancelled()) {
                        handleElementClick(clickPlayer, slotIndex, mappedClickType, context);
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
                GuiCloseHooks.close(gui, context.player(), CloseReason.UNKNOWN);
                activeGuis.remove(closePlayer.uniqueId());
            }
        });

        ActiveGui activeGui = new ActiveGui(gui, inventory, menu, context);
        if (!InventoryEventBridge.dispatchOpenPre(player, wrappedInventory)) {
            return;
        }

        activeGuis.put(player.uuid(), activeGui);

        menu.open(spongePlayer);
        InventoryEventBridge.dispatchOpen(player, wrappedInventory);
    }

    @Override
    public void close(@NotNull Gui gui, @NotNull RPlayer player) {
        ActiveGui active = activeGuis.remove(player.uuid());
        if (active != null) {
            ServerPlayer spongePlayer = resolvePlayer(player);
            if (spongePlayer != null) {
                spongePlayer.closeInventory();
            }
        }
    }

    @Nullable
    ActiveGui getActiveGui(@NotNull UUID playerId) {
        return activeGuis.get(playerId);
    }

    void removeActiveGui(@NotNull UUID playerId) {
        activeGuis.remove(playerId);
    }

    private void handleElementClick(@NotNull ServerPlayer player, int slotIndex, @NotNull ClickType clickType,
                                     @NotNull RenderContext context) {
        GuiElement element = context.elementAt(slotIndex);
        if (element == null) return;

        ClickContext clickContext = GuiContexts.click(context.player(), element, slotIndex, clickType, context.state());
        GuiInventoryElementHandler.Result result = GuiInventoryElementHandler.handle(
            element,
            clickContext,
            input -> handleInputClick(input, player, context),
            dropdown -> handleDropdownClick(dropdown, player, context)
        );
        if (result.stateMutated()) {
            refreshInventory(player, context);
        }
    }

    private void handleInputClick(@NotNull InputElement input, @NotNull ServerPlayer player, 
                                   @NotNull RenderContext context) {
        anvilInputHandler.openAnvilInput(player, input, context);
    }

    private void handleDropdownClick(@NotNull DropdownElement dropdown, @NotNull ServerPlayer player,
                                      @NotNull RenderContext context) {
        dropdownHandler.openDropdown(player, dropdown, context);
    }

    private void refreshInventory(@NotNull ServerPlayer player, @NotNull RenderContext context) {
        ActiveGui active = activeGuis.get(player.uniqueId());
        if (active != null) {
            render(active.gui, context.player(), context);
        }
    }

    @Nullable
    private ServerPlayer resolvePlayer(@NotNull RPlayer player) {
        return Sponge.server().player(player.uuid()).orElse(null);
    }

    private static @NotNull RInventory wrapInventory(@NotNull ViewableInventory inventory) {
        return InventoryFeatures.install().require(inventory);
    }

    static final class ActiveGui {
        final Gui gui;
        final ViewableInventory inventory;
        final InventoryMenu menu;
        final RenderContext context;

        ActiveGui(Gui gui, ViewableInventory inventory, InventoryMenu menu, RenderContext context) {
            this.gui = gui;
            this.inventory = inventory;
            this.menu = menu;
            this.context = context;
        }
    }

}
