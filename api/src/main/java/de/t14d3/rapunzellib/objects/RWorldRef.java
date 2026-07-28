package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * An immutable reference to a world, identified by name and/or key.
 *
 * <p>At least one of {@code name} or {@code key} must be non-null and non-blank.</p>
 *
 * @param name the world name, may be null if key is provided
 * @param key  the world key, may be null if name is provided
 */
public record RWorldRef(@Nullable String name, @Nullable RKey key) {
    public RWorldRef {
        boolean hasName = name != null && !name.isBlank();
        boolean hasKey = key != null;
        if (!hasName && !hasKey) {
            throw new IllegalArgumentException("Either name or key must be provided");
        }
    }

    /**
     * Wraps a native platform world handle into an {@code RWorldRef}, if supported.
     *
     * <p>Delegates to the registered {@link WrapperStore} which may cache the
     * returned reference by the native handle. Returns an empty
     * {@link Optional} when no store is available or the native type is
     * not supported.</p>
     *
     * @param nativeWorld the native world handle
     * @return an {@link Optional} containing the cached world reference,
     *         or empty if not supported
     */
    public static @NotNull Optional<RWorldRef> wrap(@NotNull Object nativeWorld) {
        WrapperStore store = WrapperStore.current();
        return store != null ? store.worldRef(nativeWorld) : Optional.empty();
    }

    /**
     * Wraps a native platform world handle into an {@code RWorldRef}, throwing
     * if not supported.
     *
     * @param nativeWorld the native world handle
     * @return the cached or newly-created world reference
     * @throws IllegalArgumentException if the native world cannot be wrapped
     */
    public static @NotNull RWorldRef require(@NotNull Object nativeWorld) {
        return wrap(nativeWorld).orElseThrow(() -> new IllegalArgumentException("Cannot wrap native world: " + nativeWorld));
    }

    /**
     * Wraps a native platform world handle into an {@code RWorldRef}.
     *
     * <p>Convenience alias for {@link #require(Object)} mirroring the
     * {@code of(Object)} convention used by {@link RBlock#of(Object)} and
     * other {@code RNative}-backed types.</p>
     *
     * @param nativeWorld the native world handle
     * @return the cached or newly-created world reference
     * @throws IllegalArgumentException if the native world cannot be wrapped
     */
    public static @NotNull RWorldRef of(@NotNull Object nativeWorld) {
        return require(nativeWorld);
    }

    /**
     * Creates a world reference from a key.
     *
     * @param key the world key
     * @return the new world reference
     */
    public static @NotNull RWorldRef of(@NotNull RKey key) {
        return new RWorldRef(null, key);
    }

    /**
     * Creates a world reference from a string key.
     *
     * @param key the world key string
     * @return the new world reference
     */
    public static @NotNull RWorldRef of(@NotNull String key) {
        return new RWorldRef(null, RKey.of(key));
    }

    /**
     * Creates a world reference from an optional name and a string key.
     *
     * @param name the world name, may be null
     * @param key  the world key string, may be null or blank
     * @return the new world reference
     */
    public static @NotNull RWorldRef of(@Nullable String name, @Nullable String key) {
        return new RWorldRef(name, key);
    }

    /**
     * Creates a world reference from an optional name and a key.
     *
     * @param name the world name, may be null
     * @param key  the world key, may be null
     * @return the new world reference
     */
    public static @NotNull RWorldRef of(@Nullable String name, @Nullable RKey key) {
        return new RWorldRef(name, key);
    }

    /**
     * Creates a world reference from an optional name and a string key.
     *
     * @param name the world name, may be null
     * @param key  the world key string, may be null or blank
     */
    public RWorldRef(@Nullable String name, @Nullable String key) {
        this(name, key == null || key.isBlank() ? null : RKey.of(key));
    }

    /**
     * Creates a world reference from a key.
     *
     * @param key the world key
     */
    public RWorldRef(@NotNull RKey key) {
        this(null, key);
    }

    /**
     * Returns a human-readable identifier for this world reference.
     *
     * @return the key string if available, otherwise the name
     */
    public @NotNull String identifier() {
        if (key != null) return key.asString();
        assert name != null;
        return name;
    }
}
