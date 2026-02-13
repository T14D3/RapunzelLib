package de.t14d3.rapunzellib.events.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.shared.SharedGameEventSupportManifests;
import de.t14d3.rapunzellib.events.block.*;
import de.t14d3.rapunzellib.events.entity.AttackEntityPost;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPost;
import de.t14d3.rapunzellib.events.entity.EntityHurtSnapshot;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.events.entity.InteractEntityPost;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;

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
                    EntityHurtPost.class,
                    EntityHurtSnapshot.class
                )
                .partialSupport(
                    "NeoForge entity join bridge does not expose spawn reasons",
                    EntitySpawnPre.class,
                    EntitySpawnPost.class,
                    EntitySpawnSnapshot.class
                ),
            "NeoForge event bus bridge"
        ),
        "NeoForge mixin bridge"
    ).build();

    private NeoForgeGameEventSupport() {
    }
}
