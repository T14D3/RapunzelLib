package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public interface RNbtCodec<T> {

    @NotNull T decode(@NotNull RNbtValue value);

    @NotNull RNbtValue encode(@NotNull T value);

    default @NotNull RNbtType encodedType() {
        throw new UnsupportedOperationException("Codec does not declare a single encoded type");
    }

    default @NotNull Optional<T> decodeOptional(RNbtValue value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(decode(value));
    }

    static <T> @NotNull RNbtCodec<T> of(
        @NotNull RNbtType type,
        @NotNull Encoder<T> encoder,
        @NotNull Decoder<T> decoder
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(decoder, "decoder");
        return new RNbtCodec<>() {
            @Override
            public @NotNull T decode(@NotNull RNbtValue value) {
                return decoder.decode(value);
            }

            @Override
            public @NotNull RNbtValue encode(@NotNull T value) {
                return encoder.encode(value);
            }

            @Override
            public @NotNull RNbtType encodedType() {
                return type;
            }
        };
    }

    @FunctionalInterface
    interface Encoder<T> {
        @NotNull RNbtValue encode(@NotNull T value);
    }

    @FunctionalInterface
    interface Decoder<T> {
        @NotNull T decode(@NotNull RNbtValue value);
    }
}
