package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public final class RNbtField<T> implements Serializable {
    private final @NotNull String key;
    private final @NotNull RNbtPath<T> path;

    private RNbtField(@NotNull String key, @NotNull RNbtCodec<T> codec) {
        this.key = Objects.requireNonNull(key, "key");
        this.path = RNbtPath.of(Objects.requireNonNull(codec, "codec"), key);
    }

    public static <T> @NotNull RNbtField<T> of(@NotNull String key, @NotNull RNbtCodec<T> codec) {
        return new RNbtField<>(key, codec);
    }

    public @NotNull String key() {
        return key;
    }

    public @NotNull RNbtPath<T> path() {
        return path;
    }

    public @NotNull RNbtCodec<T> codec() {
        return path.codec();
    }

    public @NotNull Optional<T> read(@NotNull RNbtCompound compound) {
        return path.read(Objects.requireNonNull(compound, "compound"));
    }

    public @NotNull RNbtCompound write(@NotNull RNbtCompound compound, @NotNull T value) {
        return path.write(Objects.requireNonNull(compound, "compound"), value);
    }

    public @NotNull RNbtCompound remove(@NotNull RNbtCompound compound) {
        return path.remove(Objects.requireNonNull(compound, "compound"));
    }

    @Override
    public String toString() {
        return key;
    }
}
