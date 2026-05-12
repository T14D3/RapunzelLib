package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Platform serializer for live block entities, converting between {@link SerializedBlockEntity} snapshots
 * and native block entity handles.
 *
 * @param <B> the native block entity type
 * @param <L> the native location/world type used for deserialization
 */
public interface BlockEntityNbtSerializer<B, L> {

    /**
     * Serializes a native block entity into a transportable snapshot.
     *
     * @param blockEntity the native block entity to serialize
     * @return the serialized block entity snapshot
     */
    @NotNull
    SerializedBlockEntity serialize(@NotNull B blockEntity);

    /**
     * Deserializes a snapshot back into a native block entity at the given location.
     *
     * @param data     the serialized block entity data
     * @param location the location/world
     * @return the deserialized native block entity
     */
    @NotNull
    B deserialize(@NotNull SerializedBlockEntity data, @NotNull L location);

    /**
     * Serializes only the NBT data portion of a native block entity.
     *
     * @param blockEntity the native block entity
     * @return the serialized NBT compound
     */
    default @NotNull RNbtCompound serializeData(@NotNull B blockEntity) {
        return serialize(blockEntity).data();
    }

    /**
     * Serializes a native block entity asynchronously.
     *
     * @param blockEntity the native block entity
     * @return a future completing with the serialized snapshot
     */
    default @NotNull CompletableFuture<SerializedBlockEntity> serializeAsync(@NotNull B blockEntity) {
        return CompletableFuture.supplyAsync(() -> serialize(blockEntity));
    }
}
