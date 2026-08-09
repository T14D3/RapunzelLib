package de.t14d3.rapunzellib.events.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.shared.SharedGameEventSupportManifests;
import de.t14d3.rapunzellib.events.block.*;
import de.t14d3.rapunzellib.events.entity.*;
import de.t14d3.rapunzellib.events.interact.UseBlockSnapshot;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPost;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.TntPrimePre;

final class FabricGameEventSupport {
    private static final String MIXIN_BRIDGE_DETAILS = "Fabric mixin bridge";
    private static final String CALLBACK_BRIDGE_DETAILS = "Fabric API callback bridge";

    static final GameEventSupportManifest MANIFEST = SharedGameEventSupportManifests.withBlockMixinBridge(
        SharedGameEventSupportManifests.withServerLifecycleBridge(
            GameEventSupportManifest.builder(PlatformId.FABRIC)
                .nativeSupport(
                    CALLBACK_BRIDGE_DETAILS,
                    BlockBreakPre.class,
                    BlockBreakPost.class,
                    BlockBreakSnapshot.class,
                    InteractBlockPre.class,
                    InteractEntityPre.class,
                    AttackEntityPre.class
                )
                .partialSupport(
                    "Derived from Fabric callbacks; post/snapshots reflect attempted interaction rather than authoritative committed state",
                    InteractBlockPost.class,
                    UseBlockSnapshot.class,
                    BlockPlacePre.class,
                    BlockPlacePost.class,
                    BlockPlaceSnapshot.class
                )
                .emulatedSupport(
                    MIXIN_BRIDGE_DETAILS,
                    InteractEntityPost.class,
                    AttackEntityPost.class,
                    PlayerMovePre.class,
                    PlayerMovePost.class,
                    EntityHurtPre.class,
                    EntityHurtPost.class,
                    EntityHurtSnapshot.class,
                    EntitySpawnPre.class,
                    EntitySpawnPost.class,
                    EntitySpawnSnapshot.class,
                    EntityTeleportPost.class,
                    EntityTeleportPre.class,
                    EntityMovePost.class,
                    BucketEmptyPre.class,
                    BucketFillPre.class,
                    BucketEntityPre.class,
                    ExplosionPre.class,
                    TntPrimePre.class,
                    InventoryActionPre.class,
                    InventoryActionPost.class,
                    InventoryOpenPre.class,
                    InventoryOpenPost.class,
                    InventoryClosePost.class
                ),
            CALLBACK_BRIDGE_DETAILS
        ),
        MIXIN_BRIDGE_DETAILS
    ).build();

    private FabricGameEventSupport() {
    }
}
