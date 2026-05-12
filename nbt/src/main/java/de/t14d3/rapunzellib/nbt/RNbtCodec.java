package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * A bidirectional codec for encoding/decoding Java objects to/from {@link RNbtValue NBT values}.
 * <p>
 * Implementations define how a Java type {@code T} is serialised as NBT and deserialised back.</p>
 *
 * @param <T> the Java type handled by this codec
 */
public interface RNbtCodec<T> {

    /**
     * Decodes an NBT value back into a Java object.
     *
     * @param value the NBT value to decode
     * @return the decoded Java object
     */
    @NotNull T decode(@NotNull RNbtValue value);

    /**
     * Encodes a Java object into an NBT value.
     *
     * @param value the Java object to encode
     * @return the encoded NBT value
     */
    @NotNull RNbtValue encode(@NotNull T value);

    /**
     * Returns the NBT type produced by this codec, if known.
     *
     * @return the encoded NBT type
     * @throws UnsupportedOperationException if the codec does not declare a single encoded type
     */
    default @NotNull RNbtType encodedType() {
        throw new UnsupportedOperationException("Codec does not declare a single encoded type");
    }

    /**
     * Safely decodes an NBT value that may be null.
     *
     * @param value the NBT value (may be null)
     * @return an Optional containing the decoded value, or empty if the input was null
     */
    default @NotNull Optional<T> decodeOptional(RNbtValue value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(decode(value));
    }

    /**
     * Creates a simple codec from explicit encoder and decoder functions.
     *
     * @param <T>     the Java type
     * @param type    the NBT type this codec produces
     * @param encoder the encoding function
     * @param decoder the decoding function
     * @return a new RNbtCodec
     */
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

    /**
     * Functional interface for encoding a Java value into NBT.
     *
     * @param <T> the Java type
     */
    @FunctionalInterface
    interface Encoder<T> {
        @NotNull RNbtValue encode(@NotNull T value);
    }

    /**
     * Functional interface for decoding an NBT value into a Java object.
     *
     * @param <T> the Java type
     */
    @FunctionalInterface
    interface Decoder<T> {
        @NotNull T decode(@NotNull RNbtValue value);
    }
}
