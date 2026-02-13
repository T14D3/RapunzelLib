package de.t14d3.rapunzellib.gui.paper.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import de.t14d3.rapunzellib.events.inventory.InventoryEventBridge;
import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCloseHooks;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.context.CloseReason;
import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.gui.context.ClickType;
import de.t14d3.rapunzellib.gui.core.GuiContexts;
import de.t14d3.rapunzellib.gui.core.GuiInventoryElementHandler;
import de.t14d3.rapunzellib.gui.element.*;
import de.t14d3.rapunzellib.gui.inventory.GuiInventoryClickTypes;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class InventoryClickListener implements Listener {

    private final JavaPlugin plugin;
    private final InventoryRenderer renderer;
    private final AnvilInputHandler anvilInputHandler;
    private final DropdownHandler dropdownHandler;

    public InventoryClickListener(@NotNull JavaPlugin plugin, @NotNull InventoryRenderer renderer) {
        this.plugin = plugin;
        this.renderer = renderer;
        this.anvilInputHandler = new AnvilInputHandler(plugin, renderer);
        this.dropdownHandler = new DropdownHandler(plugin, renderer);
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        anvilInputHandler.register();
        dropdownHandler.register();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        
        if (!(holder instanceof InventoryGuiHolder guiHolder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) {
            return;
        }

        int slot = event.getSlot();
        RenderContext context = guiHolder.context();

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryClickType eventClickType = PaperGuiClickTypes.mapInventoryClick(event.getClick());
        ClickType clickType = GuiInventoryClickTypes.fromEventClickType(eventClickType);
        RInventory wrappedInventory = wrapInventory(inventory);
        InventoryEventBridge.ClickDispatch clickDispatch = InventoryEventBridge.dispatchClick(
            RPlayer.get(player.getUniqueId()).orElseThrow(),
            wrappedInventory,
            slot,
            eventClickType
        );
        try {
            if (clickDispatch.cancelled()) {
                return;
            }
            GuiElement element = context.elementAt(slot);
            if (element == null) {
                return;
            }
            ClickContext clickContext = GuiContexts.click(
                RPlayer.get(player.getUniqueId()).orElseThrow(),
                element,
                slot,
                clickType,
                context.state()
            );
            handleClick(element, clickContext, inventory, slot, player);
        } finally {
            clickDispatch.post();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof InventoryGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof InventoryGuiHolder guiHolder)) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            RPlayer.get(player.getUniqueId()).ifPresent(rPlayer -> {
                InventoryEventBridge.dispatchClose(rPlayer, wrapInventory(inventory));
                GuiCloseHooks.close(guiHolder.gui(), rPlayer, closeReason(event));
            });
            renderer.removeHolder(player.getUniqueId());
        }
    }

    private static @NotNull CloseReason closeReason(@NotNull InventoryCloseEvent event) {
        String reason = event.getReason().name();
        return switch (reason) {
            case "PLAYER" -> CloseReason.PLAYER;
            case "PLUGIN" -> CloseReason.PLUGIN;
            case "OPEN_NEW" -> CloseReason.REPLACEMENT;
            case "DISCONNECT", "DEATH", "UNLOADED" -> CloseReason.SERVER;
            default -> CloseReason.UNKNOWN;
        };
    }

    private void handleClick(@NotNull GuiElement element, @NotNull ClickContext context, Inventory inventory, int slot, Player player) {
        GuiInventoryElementHandler.Result result = GuiInventoryElementHandler.handle(
            element,
            context,
            input -> handleInputClick(input, context, inventory, slot, player),
            dropdown -> handleDropdownClick(dropdown, context, inventory, slot, player)
        );

        if (element instanceof ButtonElement button && button.onClick() != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        if (!result.stateMutated()) {
            return;
        }

        InventoryGuiHolder holder = (InventoryGuiHolder) inventory.getHolder();
        if (holder == null) {
            return;
        }
        inventory.setItem(slot, InventoryBuilder.renderElement(element, holder.context()));
        switch (element) {
            case ToggleElement toggle -> {
                boolean value = context.state().get(toggle.key(), Boolean.class, toggle.defaultValue());
                player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, value ? 1.2f : 0.8f);
            }
            case SliderElement slider -> {
                float value = context.state().get(slider.key(), Float.class, slider.defaultValue());
                float maxValue = slider.max() <= 0.0f ? 1.0f : slider.max();
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0f, 1.0f + (value / maxValue));
            }
            default -> {
            }
        }
    }

    private void handleInputClick(@NotNull InputElement input, @NotNull ClickContext context, Inventory inventory, int slot, Player player) {
        // Do not open anvil if player already has an active session
        if (anvilInputHandler.hasActiveSession(player.getUniqueId())) {
            return;
        }
        
        InventoryGuiHolder holder = (InventoryGuiHolder) inventory.getHolder();
        if (holder == null) {
            return;
        }
        
        anvilInputHandler.openAnvilInput(player, input, holder.gui(), holder.context(), slot);
    }

    private void handleDropdownClick(@NotNull DropdownElement dropdown, @NotNull ClickContext context, Inventory inventory, int slot, Player player) {
        InventoryGuiHolder holder = (InventoryGuiHolder) inventory.getHolder();
        if (holder == null) {
            return;
        }
        
        dropdownHandler.openDropdown(player, dropdown, holder.gui(), holder.context(), slot);
    }

    private static @NotNull RInventory wrapInventory(@NotNull Inventory inventory) {
        return InventoryFeatures.install().require(inventory);
    }

}
