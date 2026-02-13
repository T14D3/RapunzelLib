package de.t14d3.rapunzellib.gui.paper.inventory;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererSelectionSupport;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiSlotPlan;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.events.inventory.InventoryEventBridge;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryRenderer implements GuiRenderer {

    private static final InventoryRenderer INSTANCE = new InventoryRenderer();
    private static final Set<GuiCapability> CAPABILITIES = GuiRendererSelectionSupport.inventoryCapabilities();

    private final Map<UUID, InventoryGuiHolder> activeGuis = new ConcurrentHashMap<>();

    private InventoryRenderer() {
    }

    public static InventoryRenderer instance() {
        return INSTANCE;
    }

    @Override
    public @NotNull String name() {
        return "inventory";
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
        Player bukkitPlayer = resolveBukkitPlayer(player);
        if (bukkitPlayer == null) {
            return;
        }

        GuiSlotPlan slotPlan = GuiSlotPlan.resolve(gui, 6);

        InventoryGuiHolder holder = new InventoryGuiHolder(gui, context);
        Component title = gui.title() != null ? gui.title() : Component.text("GUI");
        Inventory inventory = Bukkit.createInventory(holder, slotPlan.size(), title);
        holder.setInventory(inventory);

        InventoryBuilder.populateInventory(gui, context, inventory);
        RInventory wrappedInventory = wrapInventory(inventory);

        if (!InventoryEventBridge.dispatchOpenPre(player, wrappedInventory)) {
            return;
        }

        activeGuis.put(player.uuid(), holder);
        bukkitPlayer.openInventory(inventory);
        InventoryEventBridge.dispatchOpen(player, wrappedInventory);
    }

    @Override
    public void close(@NotNull Gui gui, @NotNull RPlayer player) {
        Player bukkitPlayer = resolveBukkitPlayer(player);
        if (bukkitPlayer != null && bukkitPlayer.getOpenInventory().getTopInventory().getHolder() instanceof InventoryGuiHolder) {
            bukkitPlayer.closeInventory();
        }
        activeGuis.remove(player.uuid());
    }

    @Nullable
    public InventoryGuiHolder getHolder(@NotNull UUID playerId) {
        return activeGuis.get(playerId);
    }

    public void removeHolder(@NotNull UUID playerId) {
        activeGuis.remove(playerId);
    }

    public boolean hasActiveGui(@NotNull UUID playerId) {
        return activeGuis.containsKey(playerId);
    }

    @Nullable
    private Player resolveBukkitPlayer(@NotNull RPlayer player) {
        return player.tryHandle(Player.class).orElseGet(() -> Bukkit.getPlayer(player.uuid()));
    }

    private static @NotNull RInventory wrapInventory(@NotNull Inventory inventory) {
        return InventoryFeatures.install().require(inventory);
    }
}
