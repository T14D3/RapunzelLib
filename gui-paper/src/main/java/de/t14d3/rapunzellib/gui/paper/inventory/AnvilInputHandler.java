package de.t14d3.rapunzellib.gui.paper.inventory;

import de.t14d3.rapunzellib.events.inventory.InventoryEventBridge;
import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiChildTransitions;
import de.t14d3.rapunzellib.gui.core.GuiInteractionEngine;
import de.t14d3.rapunzellib.gui.core.GuiSessionStore;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class AnvilInputHandler implements Listener {

    private final JavaPlugin plugin;
    private final InventoryRenderer renderer;
    private final GuiSessionStore<AnvilSession> activeSessions = new GuiSessionStore<>();
    private final GuiChildTransitions inTransition = new GuiChildTransitions();

    public AnvilInputHandler(@NotNull JavaPlugin plugin, @NotNull InventoryRenderer renderer) {
        this.plugin = plugin;
        this.renderer = renderer;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean hasActiveSession(@NotNull UUID playerId) {
        return activeSessions.contains(playerId) || inTransition.contains(playerId);
    }

    public void openAnvilInput(@NotNull Player player, @NotNull InputElement input, @NotNull Gui parentGui, @NotNull RenderContext parentContext, int parentSlot) {

        UUID playerId = player.getUniqueId();
        if (activeSessions.contains(playerId) || inTransition.contains(playerId)) return;

        String currentValue = parentContext.state().get(input.key(), String.class, input.defaultValue() != null ? input.defaultValue() : "");

        Inventory anvilInv = Bukkit.createInventory(null, InventoryType.ANVIL, input.label() != null ? input.label() : Component.text("Enter text"));
        RInventory wrappedInventory = wrapInventory(anvilInv);

        ItemStack renameItem = new ItemStack(Material.PAPER);
        ItemMeta meta = renameItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(currentValue.isEmpty() ? " " : currentValue));
            renameItem.setItemMeta(meta);
        }

        anvilInv.setItem(0, renameItem);

        RPlayer rPlayer = RPlayer.get(playerId).orElse(null);
        if (rPlayer != null && !InventoryEventBridge.dispatchOpenPre(rPlayer, wrappedInventory)) {
            return;
        }

        activeSessions.put(playerId, new AnvilSession(input, parentGui, parentContext));
        player.openInventory(anvilInv);
        if (rPlayer != null) {
            InventoryEventBridge.dispatchOpen(rPlayer, wrappedInventory);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView() instanceof AnvilView anvilView)) return;

        AnvilInventory anvilInventory = anvilView.getTopInventory();
        if (anvilInventory == null) return;

        AnvilSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        int rawSlot = event.getRawSlot();

        if (rawSlot < 0 || rawSlot > 2) {
            event.setCancelled(true);
            return;
        }

        InventoryEventBridge.ClickDispatch clickDispatch = InventoryEventBridge.dispatchClick(
            RPlayer.get(player.getUniqueId()).orElseThrow(),
            wrapInventory(anvilInventory),
            rawSlot,
            PaperGuiClickTypes.mapInventoryClick(event.getClick())
        );
        try {
            if (clickDispatch.cancelled()) {
                event.setCancelled(true);
                return;
            }

            // 0 = left input, 1 = right input, 2 = result
            if (rawSlot == 2) {
                // submit
                event.setCancelled(true);

                ItemStack result = anvilInventory.getResult();
                String typed = "";

                if (result != null && result.getType() != Material.AIR) {
                    String rt = anvilView.getRenameText();
                    typed = rt != null ? rt : "";
                }

                if (typed.length() > session.input.maxLength()) {
                    typed = typed.substring(0, session.input.maxLength());
                }

                UUID playerId = player.getUniqueId();
                inTransition.begin(playerId);
                activeSessions.remove(playerId);
                RPlayer rPlayer = RPlayer.get(playerId).orElseThrow();
                GuiInteractionEngine.submitInput(session.input, session.parentContext.state(), rPlayer, typed);

                // clear inventory to avoid leftovers and close
                anvilInventory.clear();
                player.closeInventory();

                reopenParentGuiLater(player, session.parentGui, playerId, 3L);
                return;
            }

            if (rawSlot == 0 || rawSlot == 1) {
                event.setCancelled(true);
            }
        } finally {
            clickDispatch.post();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.ANVIL) return;

        UUID playerId = player.getUniqueId();
        boolean managedInventory = activeSessions.contains(playerId) || inTransition.contains(playerId);
        if (!managedInventory) return;

        RPlayer.get(playerId).ifPresent(rPlayer -> InventoryEventBridge.dispatchClose(rPlayer, wrapInventory(event.getInventory())));
        if (event.getReason() == InventoryCloseEvent.Reason.PLUGIN) return;

        // Skip if already in transition (already handled by click)
        if (inTransition.contains(playerId)) return;

        AnvilSession session = activeSessions.remove(playerId);
        if (session == null) return;

        // Player pressed ESC - mark transition and reopen parent
        inTransition.begin(playerId);
        reopenParentGuiLater(player, session.parentGui, playerId, 3L);
    }

    private void reopenParentGuiLater(@NotNull Player player, @NotNull Gui parentGui, @NotNull UUID playerId, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            inTransition.end(playerId);
            RPlayer rPlayer = RPlayer.get(playerId).orElse(null);
            if (rPlayer != null) parentGui.open(rPlayer);
        }, delayTicks);
    }

    private static @NotNull RInventory wrapInventory(@NotNull Inventory inventory) {
        return InventoryFeatures.install().require(inventory);
    }

    private static final class AnvilSession {
        final InputElement input;
        final Gui parentGui;
        final RenderContext parentContext;

        AnvilSession(@NotNull InputElement input, @NotNull Gui parentGui, @NotNull RenderContext parentContext) {
            this.input = input;
            this.parentGui = parentGui;
            this.parentContext = parentContext;
        }
    }
}
