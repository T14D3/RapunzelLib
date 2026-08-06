package de.t14d3.rapunzellib.events.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.shared.SharedGameEventSupportManifests;
import de.t14d3.rapunzellib.events.block.*;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.interact.UseBlockPost;
import de.t14d3.rapunzellib.events.interact.UseBlockPre;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.TntPrimePre;

final class NeoForgeGameEventSupport {
    static final GameEventSupportManifest MANIFEST = SharedGameEventSupportManifests.withBlockMixinBridge(
        SharedGameEventSupportManifests.withServerLifecycleBridge(
            GameEventSupportManifest.builder(PlatformId.NEOFORGE)
                .nativeSupport(
                    "NeoForge event bus bridge",
                    BlockBreakPre.class,
                    BlockBreakPost.class,
                    BlockBreakSnapshot.class,
                    BlockPlacePre.class,
                    BlockPlacePost.class,
                    BlockPlaceSnapshot.class,
                    InteractBlockPre.class,
                    InteractEntityPre.class,
                    InteractEntityPost.class,
                    AttackEntityPre.class,
                    AttackEntityPost.class,
                    EntityHurtPre.class,
                    EntityHurtPost.class,
                    EntityHurtSnapshot.class,
                    UseBlockPre.class,
                    UseBlockPost.class,
                    UseBlockSnapshot.class,
                    EntityTeleportPost.class,
                    InventoryOpenPre.class,
                    InventoryOpenPost.class,
                    InventoryClosePost.class
                )
                .partialSupport(
                    "NeoForge entity join bridge does not expose spawn reasons",
                    EntitySpawnPre.class,
                    EntitySpawnPost.class,
                    EntitySpawnSnapshot.class
                )
                .emulatedSupport(
                    "NeoForge ServerExplosion.explode mixin",
                    ExplosionPre.class
                )
                .emulatedSupport(
                    "NeoForge TntBlock.wasExploded mixin",
                    TntPrimePre.class
                )
                .emulatedSupport(
                    "NeoForge BucketItem.use / Player.interactOn mixins",
                    BucketEmptyPre.class,
                    BucketFillPre.class,
                    BucketEntityPre.class
                )
                .emulatedSupport(
                    "NeoForge AbstractContainerMenu.doClick mixin",
                    InventoryClickPre.class,
                    InventoryClickPost.class
                )
                .emulatedSupport(
                    "NeoForge Entity.move() mixin",
                    EntityMovePost.class
                )
                .emulatedSupport(
                    "NeoForge Entity.move() mixin",
                    PlayerMovePre.class,
                    PlayerMovePost.class
                ),
            "NeoForge event bus bridge"
        ),
        "NeoForge mixin bridge"
    ).build();

    private NeoForgeGameEventSupport() {
    }
}
