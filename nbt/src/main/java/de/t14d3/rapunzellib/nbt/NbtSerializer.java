package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface NbtSerializer<E, L> {

    @NotNull
    SerializedEntity serialize(@NotNull E entity);

    @NotNull
    E deserialize(@NotNull SerializedEntity data, @NotNull L location);

    default @NotNull RNbtCompound serializeData(@NotNull E entity) {
        return serialize(entity).data();
    }

    default @NotNull CompletableFuture<SerializedEntity> serializeAsync(@NotNull E entity) {
        return CompletableFuture.supplyAsync(() -> serialize(entity));
    }
}
