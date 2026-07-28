package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A precise location in a world, including rotation.
 *
 * @param world the world reference
 * @param x the x-coordinate
 * @param y the y-coordinate
 * @param z the z-coordinate
 * @param yaw the yaw rotation
 * @param pitch the pitch rotation
 */
public record RLocation(@NotNull RWorldRef world, double x, double y, double z, float yaw, float pitch) {
    /**
     * Wraps a native platform location handle into an {@code RLocation}, if supported.
     *
     * <p>Delegates to the registered {@link WrapperStore} which may cache the
     * returned location by the native handle. Returns an empty
     * {@link Optional} when no store is available or the native type is not
     * supported (e.g. on platforms without a dedicated native location type).</p>
     *
     * @param nativeLocation the native location handle
     * @return an {@link Optional} containing the cached location, or empty if not supported
     */
    public static @NotNull Optional<RLocation> wrap(@NotNull Object nativeLocation) {
        WrapperStore store = WrapperStore.current();
        return store != null ? store.location(nativeLocation) : Optional.empty();
    }

    /**
     * Wraps a native platform location handle into an {@code RLocation}, throwing if not supported.
     *
     * @param nativeLocation the native location handle
     * @return the cached or newly-created location
     * @throws IllegalArgumentException if the native location cannot be wrapped
     */
    public static @NotNull RLocation require(@NotNull Object nativeLocation) {
        return wrap(nativeLocation).orElseThrow(() -> new IllegalArgumentException("Cannot wrap native location: " + nativeLocation));
    }

    /**
     * Wraps a native platform location handle into an {@code RLocation}.
     *
     * <p>Convenience alias for {@link #require(Object)} mirroring the
     * {@code of(Object)} convention used by {@link RBlock#of(Object)} and
     * other {@code RNative}-backed types. Note that coordinate-based
     * overloads {@link #of(RWorldRef, double, double, double, float, float)}
     * and {@link #of(RWorldRef, double, double, double)} remain the
     * preferred way to construct a location from raw coordinates.</p>
     *
     * @param nativeLocation the native location handle
     * @return the cached or newly-created location
     * @throws IllegalArgumentException if the native location cannot be wrapped
     */
    public static @NotNull RLocation of(@NotNull Object nativeLocation) {
        return require(nativeLocation);
    }

    /**
     * Creates a location with full coordinates and rotation.
     *
     * @param world the world reference
     * @param x     the x-coordinate
     * @param y     the y-coordinate
     * @param z     the z-coordinate
     * @param yaw   the yaw rotation
     * @param pitch the pitch rotation
     * @return the new location
     */
    public static @NotNull RLocation of(@NotNull RWorldRef world, double x, double y, double z, float yaw, float pitch) {
        return new RLocation(world, x, y, z, yaw, pitch);
    }

    /**
     * Creates a location with default yaw (0) and pitch (0).
     *
     * @param world the world reference
     * @param x     the x-coordinate
     * @param y     the y-coordinate
     * @param z     the z-coordinate
     * @return the new location
     */
    public static @NotNull RLocation of(@NotNull RWorldRef world, double x, double y, double z) {
        return new RLocation(world, x, y, z, 0f, 0f);
    }

    /**
     * Converts this location to a block position by flooring coordinates.
     *
     * @return the block position
     */
    public @NotNull RBlockPos blockPos() {
        return new RBlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    /**
     * Creates a location with default yaw (0) and pitch (0).
     *
     * @param world the world reference
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     */
    public RLocation(@NotNull RWorldRef world, double x, double y, double z) {
        this(world, x, y, z, 0f, 0f);
    }
}

