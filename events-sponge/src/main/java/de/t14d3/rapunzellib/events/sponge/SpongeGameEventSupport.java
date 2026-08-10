package de.t14d3.rapunzellib.events.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.block.BlockBreakPost;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;
import de.t14d3.rapunzellib.events.block.BlockBreakSnapshot;
import de.t14d3.rapunzellib.events.block.BlockDestroyPre;
import de.t14d3.rapunzellib.events.block.BlockFormPre;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPre;
import de.t14d3.rapunzellib.events.block.BlockPlacePost;
import de.t14d3.rapunzellib.events.block.BlockPlacePre;
import de.t14d3.rapunzellib.events.block.BlockPlaceSnapshot;
import de.t14d3.rapunzellib.events.block.BlockSpreadPre;
import de.t14d3.rapunzellib.events.block.BlockTransformPre;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPost;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPre;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPost;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
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
import de.t14d3.rapunzellib.events.inventory.InventoryActionPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;

final class SpongeGameEventSupport {
    static final GameEventSupportManifest MANIFEST = GameEventSupportManifest.builder(PlatformId.SPONGE)
        .nativeSupport(
            "Sponge event API bridge",
            BlockBreakPre.class,
            BlockBreakPost.class,
            BlockBreakSnapshot.class,
            BlockPlacePre.class,
            BlockPlacePost.class,
            BlockPlaceSnapshot.class,
            AttackEntityPre.class,
            AttackEntityPost.class,
            InteractEntityPre.class,
            InteractEntityPost.class,
            EntityHurtPost.class,
            EntityHurtPre.class,
            EntityHurtSnapshot.class,
            EntitySpawnPost.class,
            EntitySpawnPre.class,
            EntitySpawnSnapshot.class,
            UseBlockSnapshot.class,
            InteractBlockPost.class,
            PlayerQuitPost.class,
            PlayerMovePre.class,
            PlayerMovePost.class,
            BucketEmptyPre.class,
            BucketFillPre.class,
            BucketEntityPre.class,
            ChunkUnloadPost.class,
            WorldLoadPost.class,
            ExplosionPre.class,
            TntPrimePre.class,
            EntityTeleportPost.class,
            EntityTeleportPre.class,
            EntityMovePost.class,
            InventoryOpenPre.class,
            InventoryOpenPost.class,
            InventoryClosePost.class,
            InventoryActionPre.class,
            InventoryActionPost.class
        )
        .nativeSupport(
            "Sponge ChangeBlockEvent.All: non-player removals to air/fluid",
            BlockDestroyPre.class
        )
        .nativeSupport(
            "Sponge ChangeBlockEvent.All transactions classified with shared block utils",
            BlockFormPre.class,
            BlockSpreadPre.class,
            BlockTransformPre.class
        )
        .nativeSupport(
            "Sponge NotifyNeighborBlockEvent tickets (no post equivalent)",
            BlockPhysicsPre.class
        )
        .partialSupport(
            "Sponge only exposes primary/secondary block interaction here; the STEP kind (pressure plates/tripwires) is not expressible via the Sponge API",
            InteractBlockPre.class
        )
        .nativeSupport(
            "Sponge PlayerChatEvent.Submit / ExecuteCommandEvent bridge",
            PlayerMessagePost.class
        )
        .partialSupport(
            "Sponge chat Pre (PlayerChatEvent.Submit) is cancellable, but the command Pre (ExecuteCommandEvent) is not cancellable via the Sponge API - a deny cannot be honored for commands",
            PlayerMessagePre.class
        )
        .partialSupport(
            "Sponge API has no game-mode/state-change event; PlayerStatePost is not bridged on Sponge",
            PlayerStatePost.class
        )
        // InventoryTransferPre: UNSUPPORTED on Sponge. Sponge does fire
        // TransferInventoryEvent.Pre for hopper block/minecart push and pull
        // (SpongeCommon HopperBlockEntityMixin_Inventory), but the event
        // exposes only sourceInventory()/targetInventory() - no item, no
        // amount, no slots. The RLib payload requires item + amount, and
        // deriving them from the source inventory would be a guess ("do not
        // fake it"), so the event stays unbridged on this platform.
        .build();

    private SpongeGameEventSupport() {
    }
}
