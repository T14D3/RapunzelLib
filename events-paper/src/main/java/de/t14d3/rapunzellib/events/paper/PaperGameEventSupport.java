package de.t14d3.rapunzellib.events.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.block.*;
import de.t14d3.rapunzellib.events.entity.*;
import de.t14d3.rapunzellib.events.interact.UseBlockPost;
import de.t14d3.rapunzellib.events.interact.UseBlockPre;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;

final class PaperGameEventSupport {
    static final GameEventSupportManifest MANIFEST = GameEventSupportManifest.builder(PlatformId.PAPER)
        .nativeSupport(
            "Paper/Bukkit event bridge",
            BlockBreakPre.class,
            BlockBreakPost.class,
            BlockBreakSnapshot.class,
            BlockDestroyPre.class,
            BlockPlacePre.class,
            BlockPlacePost.class,
            BlockPlaceSnapshot.class,
            BlockPhysicsPre.class,
            BlockPhysicsPost.class,
            BlockFormPre.class,
            BlockSpreadPre.class,
            AttackEntityPre.class,
            AttackEntityPost.class,
            InteractEntityPre.class,
            InteractEntityPost.class,
            EntitySpawnPre.class,
            EntitySpawnPost.class,
            EntitySpawnSnapshot.class,
            EntityHurtPre.class,
            EntityHurtPost.class,
            EntityHurtSnapshot.class,
            EntityMovePost.class,
            EntityTeleportPost.class,
            UseBlockPre.class,
            UseBlockPost.class,
            UseBlockSnapshot.class,
            BucketEmptyPre.class,
            BucketFillPre.class,
            BucketEntityPre.class,
            InteractBlockPre.class,
            PlayerQuitPost.class,
            PlayerMovePre.class,
            PlayerMovePost.class,
            InventoryOpenPre.class,
            InventoryOpenPost.class,
            InventoryClosePost.class,
            InventoryClickPre.class,
            InventoryClickPost.class,
            ChunkUnloadPost.class,
            WorldLoadPost.class,
            ExplosionPre.class,
            TntPrimePre.class
        )
        .build();

    private PaperGameEventSupport() {
    }
}
