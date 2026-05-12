package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Platform serializer for live entities, converting between {@link SerializedEntity} snapshots
 * and native entity handles.
 *
 * @param <E> the native entity type
 * @param <L> the native location/world type used for deserialization
 */
public interface NbtSerializer<E, L> {

    /**
     * Serializes a native entity into a transportable snapshot.
     *
     * @param entity the native entity to serialize
     * @return the serialized entity snapshot
     */
    @NotNull
    SerializedEntity serialize(@NotNull E entity);

    /**
     * Deserializes a snapshot back into a native entity at the given location.
     *
     * @param data     the serialized entity data
     * @param location the location/world to spawn the entity at
     * @return the deserialized native entity
     */
    @NotNull
    E deserialize(@NotNull SerializedEntity data, @NotNull L location);

    /**
     * Serializes only the NBT data portion of a native entity.
     *
     * @param entity the native entity
     * @return the serialized NBT compound
     */
    default @NotNull RNbtCompound serializeData(@NotNull E entity) {
        return serialize(entity).data();
    }

    /**
     * Serializes a native entity asynchronously.
     *
     * @param entity the native entity
     * @return a future completing with the serialized snapshot
     */
    default @NotNull CompletableFuture<SerializedEntity> serializeAsync(@NotNull E entity) {
        return CompletableFuture.supplyAsync(() -> serialize(entity));
    }
}
