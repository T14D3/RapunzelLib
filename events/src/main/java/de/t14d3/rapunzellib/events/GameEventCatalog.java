package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.events.block.BlockBreakPost;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;
import de.t14d3.rapunzellib.events.block.BlockBreakSnapshot;
import de.t14d3.rapunzellib.events.block.BlockDestroyPre;
import de.t14d3.rapunzellib.events.block.BlockFormPre;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPost;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPre;
import de.t14d3.rapunzellib.events.block.BlockPlacePost;
import de.t14d3.rapunzellib.events.block.BlockPlacePre;
import de.t14d3.rapunzellib.events.block.BlockPlaceSnapshot;
import de.t14d3.rapunzellib.events.block.BlockSpreadPre;
import de.t14d3.rapunzellib.events.block.BlockTransformPre;
import de.t14d3.rapunzellib.events.block.PistonMovePre;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityDeathPost;
import de.t14d3.rapunzellib.events.entity.EntityDeathPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityTamePost;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPost;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPre;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryTransferPre;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPost;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerLoginPre;
import de.t14d3.rapunzellib.events.player.PlayerMessagePost;
import de.t14d3.rapunzellib.events.player.PlayerMessagePre;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.player.PlayerStatePost;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;

import java.util.List;

/**
 * Central catalog of all shared event types in the RapunzelLib event system.
 *
 * <p>This package-private class maintains a definitive list of event types that
 * are shared across all platform implementations. The list is used by
 * {@link GameEventSupportManifest.Builder} to ensure all known event types
 * are accounted for when building support manifests.</p>
 */
final class GameEventCatalog {
    private static final List<Class<? extends GameEvent>> SHARED_EVENT_TYPES = List.of(
        BlockBreakPre.class,
        BlockBreakPost.class,
        BlockBreakSnapshot.class,
        BlockDestroyPre.class,
        BlockPlacePre.class,
        BlockPlacePost.class,
        BlockPlaceSnapshot.class,
        BlockPhysicsPre.class,
        BlockPhysicsPost.class,
        BlockSpreadPre.class,
        BlockFormPre.class,
        BlockTransformPre.class,
        AttackEntityPre.class,
        AttackEntityPost.class,
        InteractEntityPre.class,
        InteractEntityPost.class,
        EntitySpawnPre.class,
        EntitySpawnPost.class,
        EntitySpawnSnapshot.class,
        EntityMovePost.class,
        EntityTeleportPost.class,
        EntityTeleportPre.class,
        EntityHurtPre.class,
        EntityHurtPost.class,
        EntityHurtSnapshot.class,
        EntityDeathPre.class,
        EntityDeathPost.class,
        EntityTamePost.class,
        // InventoryActionPre/Post slot contract: raw slot ids index the FULL
        // combined menu (0..inventory().size()-1, Bukkit getRawSlot()
        // semantics); inventory() wraps the full menu (top container + player
        // inventory section); currentItem = inventory().item(firstSlot)
        // guarded by the wrap bounds; cursorItem = the menu's carried item.
        InventoryActionPre.class,
        InventoryActionPost.class,
        // InventoryTransferPre contract: sourcePos = the inventory the item
        // moves OUT of (the protected surface), targetPos = the destination;
        // null when that side is not a block (e.g. hopper minecart) or
        // unresolvable; at least one side is non-null. Deny = no transfer.
        InventoryTransferPre.class,
        InventoryOpenPre.class,
        InventoryOpenPost.class,
        InventoryClosePost.class,
        UseBlockSnapshot.class,
        BucketEmptyPre.class,
        BucketFillPre.class,
        BucketEntityPre.class,
        InteractBlockPre.class,
        InteractBlockPost.class,
        PlayerQuitPost.class,
        PlayerJoinPost.class,
        PlayerLoginPre.class,
        PlayerMessagePre.class,
        PlayerMessagePost.class,
        PlayerStatePost.class,
        PlayerMovePre.class,
        PlayerMovePost.class,
        ChunkUnloadPost.class,
        WorldLoadPost.class,
        ExplosionPre.class,
        TntPrimePre.class,
        PistonMovePre.class
    );

    private GameEventCatalog() {
    }

    static List<Class<? extends GameEvent>> sharedEventTypes() {
        return SHARED_EVENT_TYPES;
    }
}
