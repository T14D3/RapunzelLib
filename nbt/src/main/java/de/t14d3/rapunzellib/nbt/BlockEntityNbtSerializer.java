package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface BlockEntityNbtSerializer<B, L> {

    @NotNull
    SerializedBlockEntity serialize(@NotNull B blockEntity);

    @NotNull
    B deserialize(@NotNull SerializedBlockEntity data, @NotNull L location);

    default @NotNull RNbtCompound serializeData(@NotNull B blockEntity) {
        return serialize(blockEntity).data();
    }

    default @NotNull CompletableFuture<SerializedBlockEntity> serializeAsync(@NotNull B blockEntity) {
        return CompletableFuture.supplyAsync(() -> serialize(blockEntity));
    }
}
