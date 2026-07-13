package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.RBlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * Post-event fired after block physics have been applied.
 *
 * @param world          the world reference
 * @param pos            the block position
 * @param blockTypeKey   the block type undergoing physics
 * @param changedTypeKey the block type that changed, triggering this physics update
 * @param cancelled      whether the physics update was cancelled
 */
public record BlockPhysicsPost(
        @NotNull RWorldRef world,
        @NotNull RBlockPos pos,
        @NotNull RKey blockTypeKey,
        @NotNull RKey changedTypeKey,
        boolean cancelled
) implements GamePostEvent {
    public BlockPhysicsPost {
        java.util.Objects.requireNonNull(world, "world");
        java.util.Objects.requireNonNull(pos, "pos");
        java.util.Objects.requireNonNull(blockTypeKey, "blockTypeKey");
        java.util.Objects.requireNonNull(changedTypeKey, "changedTypeKey");
    }

    public BlockPhysicsPost(
            @NotNull RWorldRef world,
            @NotNull RBlockPos pos,
            @NotNull String blockTypeKey,
            @NotNull String changedTypeKey,
            boolean cancelled
    ) {
        this(world, pos, RKey.of(blockTypeKey), RKey.of(changedTypeKey), cancelled);
    }
}
