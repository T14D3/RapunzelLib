package de.t14d3.rapunzellib.objects.snapshot;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable async-safe block snapshot captured from a live block wrapper.
 */
public record RBlockSnapshot(
    @NotNull RWorldRef world,
    @NotNull RBlockPos pos,
    @NotNull RRegistryRef<RBlockType> typeRef,
    @NotNull String dataString
) {
    public RBlockSnapshot {
        world = Objects.requireNonNull(world, "world");
        pos = Objects.requireNonNull(pos, "pos");
        typeRef = Objects.requireNonNull(typeRef, "typeRef");
        dataString = Objects.requireNonNull(dataString, "dataString");
    }

    /** Returns the block type key from the type reference. */
    public @NotNull RKey blockTypeKey() {
        return typeRef.key();
    }

    /** Returns the block type ID string. */
    public @NotNull String blockTypeId() {
        return blockTypeKey().asString();
    }

    /** Captures a snapshot of the given live block's current state. */
    public static @NotNull RBlockSnapshot capture(@NotNull RBlock block) {
        Objects.requireNonNull(block, "block");
        return new RBlockSnapshot(block.world().ref(), block.pos(), block.typeRef(), block.data().asString());
    }

    /** Creates a snapshot from explicit values. */
    public static @NotNull RBlockSnapshot of(@NotNull RWorldRef world, @NotNull RBlockPos pos, @NotNull RKey blockTypeKey) {
        return new RBlockSnapshot(world, pos, RBlockType.ref(blockTypeKey), blockTypeKey.asString());
    }

    /** Creates a snapshot from explicit values with a string type key. */
    public static @NotNull RBlockSnapshot of(@NotNull RWorldRef world, @NotNull RBlockPos pos, @NotNull String blockTypeKey) {
        return of(world, pos, RKey.of(blockTypeKey));
    }
}
