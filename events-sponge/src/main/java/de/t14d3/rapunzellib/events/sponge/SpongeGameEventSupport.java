package de.t14d3.rapunzellib.events.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.block.BlockBreakPost;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;
import de.t14d3.rapunzellib.events.block.BlockBreakSnapshot;
import de.t14d3.rapunzellib.events.block.BlockPlacePost;
import de.t14d3.rapunzellib.events.block.BlockPlacePre;
import de.t14d3.rapunzellib.events.block.BlockPlaceSnapshot;
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
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
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
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
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
            UseBlockPre.class,
            UseBlockPost.class,
            UseBlockSnapshot.class,
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
            EntityMovePost.class,
            InventoryOpenPre.class,
            InventoryOpenPost.class,
            InventoryClosePost.class,
            InventoryClickPre.class,
            InventoryClickPost.class
        )
        .partialSupport(
            "Sponge only exposes primary block interaction here",
            InteractBlockPre.class
        )
        .build();

    private SpongeGameEventSupport() {
    }
}
