package de.t14d3.rapunzellib.gui.shared.map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The protocol side of map rendering: packet construction and the fake map
 * item.
 * <p>
 * Map ids are allocated counting down from {@code Integer.MAX_VALUE} so they
 * can never collide with a real map the client has seen, and nothing is
 * persisted. The player's hand item is swapped for a filled map carrying that
 * id and restored when the session closes.
 * </p>
 */
public final class SharedMapTransport {

    /** Counts down so fake ids never collide with real, server-allocated map ids. */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(Integer.MAX_VALUE);

    private SharedMapTransport() {
    }

    public static MapId allocateId() {
        return new MapId(NEXT_ID.getAndDecrement());
    }

    /**
     * Swaps the player's hotbar for the fake map item on the client only.
     * <p>
     * The server-side inventory is never touched: the player keeps their real
     * item, so ordinary interaction events still fire and can be turned into
     * map clicks. Only the client is told the hotbar holds a map - every
     * hotbar slot gets the same item, so scrolling cannot reveal the truth.
     * The map id is fake (never registered), so no map data exists anywhere.
     * </p>
     *
     * @param player the player
     * @param mapId  the fake map id the client-side item must carry
     */
    public static void showFakeMap(ServerPlayer player, MapId mapId) {
        ItemStack fake = fakeMapItem(mapId);
        for (int slot = InventoryMenu.USE_ROW_SLOT_START; slot < InventoryMenu.USE_ROW_SLOT_END; slot++) {
            sendSlot(player, slot, fake);
        }
    }

    /** Restores the player's real hotbar on the client. */
    public static void hideFakeMap(ServerPlayer player) {
        for (int slot = InventoryMenu.USE_ROW_SLOT_START; slot < InventoryMenu.USE_ROW_SLOT_END; slot++) {
            sendSlot(player, slot, player.getInventory().getItem(slot - InventoryMenu.USE_ROW_SLOT_START));
        }
    }

    /**
     * Re-asserts the fake hotbar on the client.
     * <p>
     * Any inventory resync (a denied interaction, a respawn, a plugin calling
     * {@code updateInventory}) resends the real slots and would reveal the
     * truth. Re-asserting periodically keeps the pretence up without touching
     * any server state.
     * </p>
     */
    public static void reassertFakeMap(ServerPlayer player, MapId mapId) {
        showFakeMap(player, mapId);
    }

    private static ItemStack fakeMapItem(MapId mapId) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.MAP_ID, mapId);
        return map;
    }

    private static void sendSlot(ServerPlayer player, int slot, ItemStack stack) {
        // Container 0 is the player's own inventory view, and the state id
        // must advance with every update like the synchronizer's would.
        player.connection.send(new ClientboundContainerSetSlotPacket(0, player.inventoryMenu.incrementStateId(), slot, stack));
    }

    /** Sends the full frame, e.g. immediately after the item changes hands. */
    public static void sendFull(ServerPlayer player, MapId mapId, SharedMapSurface surface) {
        sendPatch(player, mapId, surface, 0, 0, surface.width(), surface.height());
    }

    /** Sends the current dirty rectangle of the surface, if any. */
    public static boolean sendDirty(ServerPlayer player, MapId mapId, SharedMapSurface surface) {
        de.t14d3.rapunzellib.gui.map.GuiMapRect dirty = surface.dirtyRect();
        if (dirty == null) {
            return false;
        }
        sendPatch(player, mapId, surface, dirty.x(), dirty.y(), dirty.width(), dirty.height());
        return true;
    }

    private static void sendPatch(ServerPlayer player, MapId mapId, SharedMapSurface surface, int x, int y, int width, int height) {
        byte[] region = surface.region(new de.t14d3.rapunzellib.gui.map.GuiMapRect(x, y, width, height));
        MapItemSavedData.MapPatch patch = new MapItemSavedData.MapPatch(x, y, width, height, region);
        ClientboundMapItemDataPacket packet = new ClientboundMapItemDataPacket(
            mapId,
            (byte) 0,
            false,
            Optional.empty(),
            Optional.of(patch)
        );
        player.connection.send(packet);
    }
}
