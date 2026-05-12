package de.t14d3.rapunzellib.events.shared;

import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.block.BlockFormPre;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPost;
import de.t14d3.rapunzellib.events.block.BlockPhysicsPre;
import de.t14d3.rapunzellib.events.block.BlockSpreadPre;
import de.t14d3.rapunzellib.events.block.BlockTransformPre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Shared factory methods for building {@link GameEventSupportManifest} instances.
 * <p>
 * Provides convenience builders for server lifecycle and block mixin event bridges.
 */
public final class SharedGameEventSupportManifests {
    private SharedGameEventSupportManifests() {
    }

    /**
     * Adds server lifecycle native support events (player quit, chunk unload, world load).
     *
     * @param builder the manifest builder
     * @param details a description of the support
     * @return the builder for chaining
     */
    public static @NotNull GameEventSupportManifest.Builder withServerLifecycleBridge(
        @NotNull GameEventSupportManifest.Builder builder,
        @NotNull String details
    ) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(details, "details");
        return builder.nativeSupport(details, PlayerQuitPost.class, ChunkUnloadPost.class, WorldLoadPost.class);
    }

    /**
     * Adds block mixin emulated support events (physics, spread, form, transform).
     *
     * @param builder the manifest builder
     * @param details a description of the support
     * @return the builder for chaining
     */
    public static @NotNull GameEventSupportManifest.Builder withBlockMixinBridge(
        @NotNull GameEventSupportManifest.Builder builder,
        @NotNull String details
    ) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(details, "details");
        return builder.emulatedSupport(
            details,
            BlockPhysicsPre.class,
            BlockPhysicsPost.class,
            BlockSpreadPre.class,
            BlockFormPre.class,
            BlockTransformPre.class
        );
    }
}
