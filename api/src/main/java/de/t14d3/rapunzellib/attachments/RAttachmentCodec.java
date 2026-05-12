package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

/**
 * A codec for encoding and decoding attachment values for persistent storage.
 *
 * @param <T> the value type
 */
public interface RAttachmentCodec<T> {
    /**
     * Encodes a value into a byte array.
     *
     * @param value the value to encode
     * @return the encoded bytes
     */
    byte @NotNull [] encode(@NotNull T value);

    /**
     * Decodes a value from a byte array.
     *
     * @param bytes the bytes to decode
     * @return the decoded value
     */
    @NotNull T decode(byte @NotNull [] bytes);
}
