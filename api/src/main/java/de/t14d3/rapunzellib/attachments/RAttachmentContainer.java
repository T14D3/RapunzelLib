package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface RAttachmentContainer {
    <T> @NotNull Optional<T> get(@NotNull RAttachmentKey<T> key);

    <T> void put(@NotNull RAttachmentKey<T> key, @NotNull T value);

    <T> @NotNull Optional<T> remove(@NotNull RAttachmentKey<T> key);

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

    static @NotNull RAttachmentContainer empty() {
        return EmptyAttachmentContainer.INSTANCE;
    }

    static @NotNull RAttachmentContainer mutable() {
        return new MapAttachmentContainer();
    }

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
