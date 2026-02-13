package de.t14d3.rapunzellib.gui.paper.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryEventBridge;
import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiInteractionEngine;
import de.t14d3.rapunzellib.gui.core.GuiSessionStore;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Handles dropdown selection via sub-menu inventory.
 */
public final class DropdownHandler implements Listener {

    private final JavaPlugin plugin;
    private final InventoryRenderer renderer;
    private final GuiSessionStore<DropdownSession> activeDropdowns = new GuiSessionStore<>();

    public DropdownHandler(@NotNull JavaPlugin plugin, @NotNull InventoryRenderer renderer) {
        this.plugin = plugin;
        this.renderer = renderer;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openDropdown(@NotNull Player player, @NotNull DropdownElement dropdown, 
                             @NotNull Gui parentGui, @NotNull RenderContext parentContext, int parentSlot) {
        List<Option> options = dropdown.options();
        if (options.isEmpty()) {
            player.sendMessage(Component.text("No options available.", NamedTextColor.RED));
            return;
        }

        int size = ((options.size() / 9) + 1) * 9;
        Component title = dropdown.label() != null ? dropdown.label() : Component.text("Select Option");
        
        DropdownHolder holder = new DropdownHolder();
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);
        RInventory wrappedInventory = wrapInventory(inventory);
        
        String selectedId = parentContext.state().get(dropdown.key(), String.class);
        
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            boolean isSelected = option.id().equals(selectedId);
            inventory.setItem(i, InventoryBuilder.renderDropdownOption(option, isSelected));
        }

        RPlayer rPlayer = RPlayer.get(player.getUniqueId()).orElse(null);
        if (rPlayer != null && !InventoryEventBridge.dispatchOpenPre(rPlayer, wrappedInventory)) {
            return;
        }

        activeDropdowns.put(player.getUniqueId(), new DropdownSession(dropdown, parentGui, parentContext));
        player.openInventory(inventory);
        if (rPlayer != null) {
            InventoryEventBridge.dispatchOpen(rPlayer, wrappedInventory);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDropdownClick(@NotNull InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        
        if (!(holder instanceof DropdownHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() != inventory) {
            return;
        }

        int slot = event.getSlot();
        DropdownSession session = activeDropdowns.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        InventoryEventBridge.ClickDispatch clickDispatch = InventoryEventBridge.dispatchClick(
            RPlayer.get(player.getUniqueId()).orElseThrow(),
            wrapInventory(inventory),
            slot,
            PaperGuiClickTypes.mapInventoryClick(event.getClick())
        );
        try {
            if (clickDispatch.cancelled()) {
                return;
            }

            DropdownElement dropdown = session.dropdown;
            List<Option> options = dropdown.options();

            if (slot < 0 || slot >= options.size()) {
                return;
            }

            Option selectedOption = options.get(slot);
            RPlayer rPlayer = RPlayer.get(player.getUniqueId()).orElse(null);
            if (rPlayer != null) {
                GuiInteractionEngine.selectDropdown(dropdown, session.parentContext.state(), rPlayer, selectedOption);
            }

            activeDropdowns.remove(player.getUniqueId());

            // Return to parent GUI
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                session.parentGui.open(RPlayer.get(player.getUniqueId()).orElseThrow());
            }, 1L);
        } finally {
            clickDispatch.post();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDropdownClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof DropdownHolder) {
            if (event.getPlayer() instanceof Player player) {
                RPlayer.get(player.getUniqueId()).ifPresent(rPlayer -> InventoryEventBridge.dispatchClose(rPlayer, wrapInventory(event.getInventory())));
                DropdownSession session = activeDropdowns.remove(player.getUniqueId());
                if (session != null) {
                    // Return to parent GUI
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        session.parentGui.open(RPlayer.get(player.getUniqueId()).orElseThrow());
                    }, 1L);
                }
            }
        }
    }

    private static class DropdownHolder implements InventoryHolder {
        private Inventory inventory;

        private void setInventory(@NotNull Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return Objects.requireNonNull(inventory, "Dropdown inventory is not initialized");
        }
    }

    private static @NotNull RInventory wrapInventory(@NotNull Inventory inventory) {
        return InventoryFeatures.install().require(inventory);
    }

    private record DropdownSession(
        DropdownElement dropdown,
        Gui parentGui,
        RenderContext parentContext
    ) {}
}
