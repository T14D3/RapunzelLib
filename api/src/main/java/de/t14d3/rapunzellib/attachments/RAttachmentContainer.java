package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A container for typed attachment values associated with a native object.
 *
 * <p>Supports transient (in-memory) attachments by default. Platform implementations
 * may also support persistent (disk-backed) attachments.</p>
 */
public interface RAttachmentContainer {
    /**
     * Retrieves an attachment value by its key.
     *
     * <p>Transient attachments are returned from in-memory storage. Persistent
     * attachments (if supported by the container) are loaded from disk.</p>
     *
     * @param key the attachment key identifying the value (including scope and type)
     * @param <T> the expected value type
     * @return an {@link Optional} containing the value, or empty if absent or the scope is unsupported
     */
    <T> @NotNull Optional<T> get(@NotNull RAttachmentKey<T> key);

    /**
     * Stores an attachment value by its key.
     *
     * <p>Transient attachments are stored in memory. If the key specifies a
     * persistent scope and the container supports it, the value is persisted to disk.
     * Throws if the given scope is not supported by this container.</p>
     *
     * @param key   the attachment key (scope determines storage strategy)
     * @param value the value to store (must match the key's type)
     * @param <T>   the value type
     */
    <T> void put(@NotNull RAttachmentKey<T> key, @NotNull T value);

    /**
     * Removes an attachment by its key and returns the previous value.
     *
     * <p>For transient keys, removes from in-memory storage. For persistent keys,
     * removes from disk-backed storage. Returns empty if the scope is unsupported
     * or no value was present.</p>
     *
     * @param key the attachment key identifying the value to remove
     * @param <T> the expected value type
     * @return an {@link Optional} containing the previous value, or empty if absent
     */
    <T> @NotNull Optional<T> remove(@NotNull RAttachmentKey<T> key);

    /**
     * Checks whether this container supports the given attachment scope.
     *
     * <p>All containers support {@link RAttachmentScope#TRANSIENT}. Persistent
     * support depends on the platform implementation.</p>
     *
     * @param scope the attachment scope to check
     * @return true if the scope is supported, false otherwise
     */
    default boolean supports(@NotNull RAttachmentScope scope) {
        Objects.requireNonNull(scope, "scope");
        return scope == RAttachmentScope.TRANSIENT;
    }

    default boolean supports(@NotNull RAttachmentKey<?> key) {
        return supports(Objects.requireNonNull(key, "key").scope());
    }

    default @NotNull AttachmentStorageSupport support() {
        return AttachmentStorageSupport.of(
            supports(RAttachmentScope.TRANSIENT),
            supports(RAttachmentScope.PERSISTENT)
        );
    }

    /**
     * Returns an empty, immutable attachment container.
     *
     * <p>All {@code put} and {@code remove} operations throw
     * {@link UnsupportedOperationException}.</p>
     *
     * @return an empty, immutable attachment container
     */
    static @NotNull RAttachmentContainer empty() {
        return EmptyAttachmentContainer.INSTANCE;
    }

    /**
     * Creates a new mutable attachment container backed by a {@link java.util.concurrent.ConcurrentHashMap}.
     *
     * <p>Supports transient attachments only. Thread-safe for concurrent access.</p>
     *
     * @return a new mutable attachment container
     */
    static @NotNull RAttachmentContainer mutable() {
        return new MapAttachmentContainer();
    }

    /**
     * Creates a new lazy-initialized mutable attachment container.
     *
     * <p>The backing storage is allocated only on the first {@code put} operation,
     * making this suitable for fields that may never receive attachments.</p>
     *
     * @return a new lazy-initialized attachment container
     */
    static @NotNull RAttachmentContainer lazyMutable() {
        return new LazyAttachmentContainer();
    }
}

final class EmptyAttachmentContainer implements RAttachmentContainer, AttachmentMapAccess {
    static final EmptyAttachmentContainer INSTANCE = new EmptyAttachmentContainer();

    private EmptyAttachmentContainer() {
    }

    @Override
    public <T> @NotNull Optional<T> get(@NotNull RAttachmentKey<T> key) {
        Objects.requireNonNull(key, "key");
        return Optional.empty();
    }

    @Override
    public <T> void put(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        throw AttachmentContainerSupport.unsupported(Objects.requireNonNull(key, "key"));
    }

    @Override
    public <T> @NotNull Optional<T> remove(@NotNull RAttachmentKey<T> key) {
        throw AttachmentContainerSupport.unsupported(Objects.requireNonNull(key, "key"));
    }

    @Override
    public @NotNull Map<RAttachmentKey<?>, Object> transientEntries() {
        return Collections.emptyMap();
    }
}

final class MapAttachmentContainer implements RAttachmentContainer, AttachmentMapAccess {
    private final ConcurrentHashMap<RAttachmentKey<?>, Object> values = new ConcurrentHashMap<>();

    @Override
    public <T> @NotNull Optional<T> get(@NotNull RAttachmentKey<T> key) {
        Objects.requireNonNull(key, "key");
        if (key.scope() != RAttachmentScope.TRANSIENT) {
            return Optional.empty();
        }
        Object value = values.get(key);
        return value == null ? Optional.empty() : Optional.of(key.type().cast(value));
    }

    @Override
    public <T> void put(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        Objects.requireNonNull(key, "key");
        AttachmentContainerSupport.validateTransientWrite(key, value);
        values.put(key, value);
    }

    @Override
    public <T> @NotNull Optional<T> remove(@NotNull RAttachmentKey<T> key) {
        Objects.requireNonNull(key, "key");
        if (key.scope() != RAttachmentScope.TRANSIENT) {
            return Optional.empty();
        }
        Object value = values.remove(key);
        return value == null ? Optional.empty() : Optional.of(key.type().cast(value));
    }

    @Override
    public @NotNull Map<RAttachmentKey<?>, Object> transientEntries() {
        return Collections.unmodifiableMap(values);
    }
}

final class LazyAttachmentContainer implements RAttachmentContainer, AttachmentMapAccess {
    private volatile MapAttachmentContainer delegate;

    private @NotNull MapAttachmentContainer ensureDelegate() {
        MapAttachmentContainer current = delegate;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = delegate;
            if (current != null) {
                return current;
            }
            current = new MapAttachmentContainer();
            delegate = current;
            return current;
        }
    }

    @Override
    public <T> @NotNull Optional<T> get(@NotNull RAttachmentKey<T> key) {
        MapAttachmentContainer current = delegate;
        return current == null ? Optional.empty() : current.get(key);
    }

    @Override
    public <T> void put(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        ensureDelegate().put(key, value);
    }

    @Override
    public <T> @NotNull Optional<T> remove(@NotNull RAttachmentKey<T> key) {
        MapAttachmentContainer current = delegate;
        return current == null ? Optional.empty() : current.remove(key);
    }

    @Override
    public @NotNull Map<RAttachmentKey<?>, Object> transientEntries() {
        MapAttachmentContainer current = delegate;
        return current == null ? Collections.emptyMap() : current.transientEntries();
    }
}

final class AttachmentContainerSupport {
    private AttachmentContainerSupport() {
    }

    static <T> void validateTransientWrite(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        Objects.requireNonNull(value, "value");
        if (key.scope() != RAttachmentScope.TRANSIENT) {
            throw unsupported(key);
        }
        if (!key.type().isInstance(value)) {
            throw new ClassCastException(
                "Value for " + key.id() + " must be of type " + key.type().getName() + " but was " + value.getClass().getName()
            );
        }
    }

    static @NotNull UnsupportedOperationException unsupported(@NotNull RAttachmentKey<?> key) {
        return new UnsupportedOperationException("attachment scope " + key.scope() + " is not supported by this container");
    }
}
