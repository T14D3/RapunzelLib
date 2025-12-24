package de.t14d3.rapunzellib.objects;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface RExtras {
    <T> Optional<T> get(RExtraKey<T> key);

    <T> void put(RExtraKey<T> key, T value);

    <T> Optional<T> remove(RExtraKey<T> key);

    Map<RExtraKey<?>, Object> asMap();

    static RExtras empty() {
        return EmptyRExtras.INSTANCE;
    }

    static RExtras mutable() {
        return new MapRExtras();
    }

    /**
     * Returns a mutable extras instance that avoids allocating its internal map until
     * an element is actually put.
     */
    static RExtras lazyMutable() {
        return new LazyRExtras();
    }
}

final class EmptyRExtras implements RExtras {
    static final EmptyRExtras INSTANCE = new EmptyRExtras();

    private EmptyRExtras() {
    }

    @Override
    public <T> Optional<T> get(RExtraKey<T> key) {
        Objects.requireNonNull(key, "key");
        return Optional.empty();
    }

    @Override
    public <T> void put(RExtraKey<T> key, T value) {
        throw new UnsupportedOperationException("extras are not supported by this wrapper instance");
    }

    @Override
    public <T> Optional<T> remove(RExtraKey<T> key) {
        throw new UnsupportedOperationException("extras are not supported by this wrapper instance");
    }

    @Override
    public Map<RExtraKey<?>, Object> asMap() {
        return Collections.emptyMap();
    }
}

final class MapRExtras implements RExtras {
    private final ConcurrentHashMap<RExtraKey<?>, Object> values = new ConcurrentHashMap<>();

    @Override
    public <T> Optional<T> get(RExtraKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object value = values.get(key);
        if (value == null) return Optional.empty();
        return Optional.of(key.type().cast(value));
    }

    @Override
    public <T> void put(RExtraKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new ClassCastException(
                "Value for " + key.id() + " must be of type " + key.type().getName() + " but was " + value.getClass().getName()
            );
        }
        values.put(key, value);
    }

    @Override
    public <T> Optional<T> remove(RExtraKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object value = values.remove(key);
        if (value == null) return Optional.empty();
        return Optional.of(key.type().cast(value));
    }

    @Override
    public Map<RExtraKey<?>, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }
}

final class LazyRExtras implements RExtras {
    private volatile MapRExtras delegate;

    private MapRExtras ensureDelegate() {
        MapRExtras current = delegate;
        if (current != null) return current;
        synchronized (this) {
            current = delegate;
            if (current != null) return current;
            current = new MapRExtras();
            delegate = current;
            return current;
        }
    }

    @Override
    public <T> Optional<T> get(RExtraKey<T> key) {
        Objects.requireNonNull(key, "key");
        MapRExtras current = delegate;
        return (current != null) ? current.get(key) : Optional.empty();
    }

    @Override
    public <T> void put(RExtraKey<T> key, T value) {
        ensureDelegate().put(key, value);
    }

    @Override
    public <T> Optional<T> remove(RExtraKey<T> key) {
        MapRExtras current = delegate;
        if (current == null) return Optional.empty();
        return current.remove(key);
    }

    @Override
    public Map<RExtraKey<?>, Object> asMap() {
        MapRExtras current = delegate;
        return (current != null) ? current.asMap() : Collections.emptyMap();
    }
}
