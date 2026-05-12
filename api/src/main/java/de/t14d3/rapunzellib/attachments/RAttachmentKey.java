package de.t14d3.rapunzellib.attachments;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A typed key for identifying attachment values, with an associated scope and optional codec.
 *
 * @param id    the unique key identifier
 * @param type  the value type class
 * @param scope the attachment scope
 * @param codec the codec for persistence, may be null for transient or directly-persistable types
 * @param <T>   the value type
 */
@SuppressWarnings("PatternValidation")
public record RAttachmentKey<T>(
    @NotNull Key id,
    @NotNull Class<T> type,
    @NotNull RAttachmentScope scope,
    @Nullable RAttachmentCodec<T> codec
) {
    public RAttachmentKey {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(scope, "scope");
        if (scope == RAttachmentScope.PERSISTENT && codec == null && !supportsDirectPersistence(type)) {
            throw new IllegalArgumentException(
                "Persistent attachment key " + id + " of type " + type.getName() + " requires a codec"
            );
        }
    }

    /**
     * Creates a transient attachment key from a {@link Key} and type.
     *
     * @param id   the key identifier
     * @param type the value type class
     * @param <T>  the value type
     * @return the attachment key
     */
    public static <T> @NotNull RAttachmentKey<T> of(@NotNull Key id, @NotNull Class<T> type) {
        return transientKey(id, type);
    }

    /**
     * Creates a transient attachment key from a string and type.
     *
     * @param id   the key identifier string
     * @param type the value type class
     * @param <T>  the value type
     * @return the attachment key
     */
    public static <T> @NotNull RAttachmentKey<T> of(@NotNull String id, @NotNull Class<T> type) {
        return transientKey(Key.key(id), type);
    }

    public static <T> @NotNull RAttachmentKey<T> transientKey(@NotNull Key id, @NotNull Class<T> type) {
        return new RAttachmentKey<>(id, type, RAttachmentScope.TRANSIENT, null);
    }

    public static <T> @NotNull RAttachmentKey<T> transientKey(@NotNull String id, @NotNull Class<T> type) {
        return transientKey(Key.key(id), type);
    }

    public static <T> @NotNull RAttachmentKey<T> persistent(@NotNull Key id, @NotNull Class<T> type) {
        return new RAttachmentKey<>(id, type, RAttachmentScope.PERSISTENT, null);
    }

    public static <T> @NotNull RAttachmentKey<T> persistent(@NotNull String id, @NotNull Class<T> type) {
        return persistent(Key.key(id), type);
    }

    public static <T> @NotNull RAttachmentKey<T> persistent(
        @NotNull Key id,
        @NotNull Class<T> type,
        @NotNull RAttachmentCodec<T> codec
    ) {
        return new RAttachmentKey<>(id, type, RAttachmentScope.PERSISTENT, Objects.requireNonNull(codec, "codec"));
    }

    public static <T> @NotNull RAttachmentKey<T> persistent(
        @NotNull String id,
        @NotNull Class<T> type,
        @NotNull RAttachmentCodec<T> codec
    ) {
        return persistent(Key.key(id), type, codec);
    }

    /**
     * Checks whether this key is persistent.
     *
     * @return true if the scope is PERSISTENT
     */
    public boolean persistent() {
        return scope == RAttachmentScope.PERSISTENT;
    }

    /**
     * Checks whether the given type can be persisted directly without a codec.
     *
     * @param type the type to check
     * @return true if the type is directly persistable
     */
    public static boolean supportsDirectPersistence(@NotNull Class<?> type) {
        Objects.requireNonNull(type, "type");
        return type == String.class
            || type == UUID.class
            || type == byte[].class
            || type == Boolean.class
            || type == Byte.class
            || type == Short.class
            || type == Integer.class
            || type == Long.class
            || type == Float.class
            || type == Double.class;
    }
}
