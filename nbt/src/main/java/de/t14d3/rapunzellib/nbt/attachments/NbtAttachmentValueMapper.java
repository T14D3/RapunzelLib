package de.t14d3.rapunzellib.nbt.attachments;

import de.t14d3.rapunzellib.attachments.RAttachmentCodec;
import de.t14d3.rapunzellib.attachments.RAttachmentKey;
import de.t14d3.rapunzellib.nbt.RNbtByteArray;
import de.t14d3.rapunzellib.nbt.RNbtCodecs;
import de.t14d3.rapunzellib.nbt.RNbtPrimitive;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Utility for encoding/decoding {@link de.t14d3.rapunzellib.attachments.RAttachmentKey attachment}
 * values to/from {@link RNbtValue NBT values} for persistent storage on items.
 * <p>
 * Supports standard Java types (String, UUID, Boolean, numeric primitives, byte[])
 * and custom {@link de.t14d3.rapunzellib.attachments.RAttachmentCodec codecs}.</p>
 */
public final class NbtAttachmentValueMapper {
    private NbtAttachmentValueMapper() {
    }

    /**
     * Encodes a Java attachment value into an {@link RNbtValue} for persistent storage.
     *
     * @param <T>   the value type
     * @param key   the attachment key (provides type and optional codec)
     * @param value the value to encode
     * @return the encoded NBT value
     * @throws ClassCastException if the value is not of the declared type
     * @throws IllegalArgumentException if the type is unsupported
     */
    public static <T> @NotNull RNbtValue encode(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new ClassCastException(
                "Value for " + key.id() + " must be of type " + key.type().getName() + " but was " + value.getClass().getName()
            );
        }
        RAttachmentCodec<T> codec = key.codec();
        if (codec != null) {
            return new RNbtByteArray(codec.encode(value));
        }
        if (key.type() == String.class) return RNbtPrimitive.ofString((String) value);
        if (key.type() == UUID.class) return RNbtPrimitive.ofString(((UUID) value).toString());
        if (key.type() == Boolean.class) return RNbtPrimitive.ofBoolean((Boolean) value);
        if (key.type() == Byte.class) return RNbtPrimitive.ofByte((Byte) value);
        if (key.type() == Short.class) return RNbtPrimitive.ofShort((Short) value);
        if (key.type() == Integer.class) return RNbtPrimitive.ofInt((Integer) value);
        if (key.type() == Long.class) return RNbtPrimitive.ofLong((Long) value);
        if (key.type() == Float.class) return RNbtPrimitive.ofFloat((Float) value);
        if (key.type() == Double.class) return RNbtPrimitive.ofDouble((Double) value);
        if (key.type() == byte[].class) return new RNbtByteArray((byte[]) value);
        throw new IllegalArgumentException("Unsupported persistent attachment type " + key.type().getName() + " for " + key.id().asString());
    }

    /**
     * Decodes an attachment value from an {@link RNbtValue}.
     *
     * @param <T>   the value type
     * @param key   the attachment key (provides type and optional codec)
     * @param value the NBT value to decode
     * @return the decoded Java value
     * @throws IllegalArgumentException if the type is unsupported
     */
    public static <T> @NotNull T decode(@NotNull RAttachmentKey<T> key, @NotNull RNbtValue value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        RAttachmentCodec<T> codec = key.codec();
        if (codec != null) {
            return codec.decode(((RNbtByteArray) value).value());
        }
        if (key.type() == String.class) return key.type().cast(RNbtCodecs.STRING.decode(value));
        if (key.type() == UUID.class) return key.type().cast(UUID.fromString(RNbtCodecs.STRING.decode(value)));
        if (key.type() == Boolean.class) return key.type().cast(RNbtCodecs.BOOLEAN.decode(value));
        if (key.type() == Byte.class) return key.type().cast(RNbtCodecs.BYTE.decode(value));
        if (key.type() == Short.class) return key.type().cast(RNbtCodecs.SHORT.decode(value));
        if (key.type() == Integer.class) return key.type().cast(RNbtCodecs.INT.decode(value));
        if (key.type() == Long.class) return key.type().cast(RNbtCodecs.LONG.decode(value));
        if (key.type() == Float.class) return key.type().cast(RNbtCodecs.FLOAT.decode(value));
        if (key.type() == Double.class) return key.type().cast(RNbtCodecs.DOUBLE.decode(value));
        if (key.type() == byte[].class) return key.type().cast(RNbtCodecs.BYTE_ARRAY.decode(value));
        throw new IllegalArgumentException("Unsupported persistent attachment type " + key.type().getName() + " for " + key.id().asString());
    }
}
