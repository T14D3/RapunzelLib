package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Per-platform cached factory for value-type wrapper objects such as
 * {@link RLocation} and {@link RWorldRef}.
 *
 * <p>Each platform registers a single {@code WrapperStore} via the
 * {@link de.t14d3.rapunzellib.context.ServiceRegistry ServiceRegistry}.
 * The store caches immutable value wrappers by their native platform
 * counterparts (e.g. a Bukkit {@code Location} mapping to an
 * {@link RLocation}), avoiding repeated allocation when the same native
 * object is wrapped multiple times within a hot path.</p>
 *
 * <p>Static factory methods on value types ({@link RLocation#of(Object)},
 * {@link RWorldRef#of(Object)}) consult the registered store first and
 * fall back to direct construction when no store is available or the
 * native type is unsupported.</p>
 *
 * <p>Platform implementations live in the platform modules
 * ({@code PaperWrapperStore}, {@code FabricWrapperStore}, etc.).</p>
 */
public interface WrapperStore {
    /**
     * Returns the currently registered {@code WrapperStore}, or {@code null}
     * if no context is available or no store is registered.
     *
     * <p>This is intended for use by static factory methods on value types
     * that need to consult the store before constructing a new instance.
     * Callers should not retain the returned reference across context
     * boundaries.</p>
     *
     * @return the current store, or {@code null}
     */
    static @Nullable WrapperStore current() {
        RapunzelContext context = Rapunzel.findContext().orElse(null);
        if (context == null) return null;
        return context.services().find(WrapperStore.class).orElse(null);
    }

    /**
     * Resolves a cached {@link RWorldRef} for the given native world handle.
     *
     * <p>Implementations are expected to cache by reference identity of the
     * native world, returning the same {@code RWorldRef} instance for the
     * same native world on subsequent calls.</p>
     *
     * @param nativeWorld the native world handle (e.g. Bukkit {@code World},
     *                    NMS {@code ServerLevel}, Sponge {@code ServerWorld})
     * @return an {@link Optional} containing the cached world reference,
     *         or empty if the native type is unsupported by this store
     */
    @NotNull Optional<RWorldRef> worldRef(@NotNull Object nativeWorld);

    /**
     * Resolves a cached {@link RLocation} for the given native location handle.
     *
     * <p>Implementations are expected to cache by reference identity (or
     * value equality when the native type provides it) of the native
     * location, returning the same {@code RLocation} instance for the same
     * native location on subsequent calls.</p>
     *
     * <p>Platforms without a dedicated native location type (e.g. Fabric,
     * NeoForge) may return an empty {@link Optional} here and rely on
     * callers constructing {@code RLocation} directly from coordinates.</p>
     *
     * @param nativeLocation the native location handle (e.g. Bukkit
     *                       {@code Location}, Sponge {@code ServerLocation})
     * @return an {@link Optional} containing the cached location,
     *         or empty if the native type is unsupported by this store
     */
    @NotNull Optional<RLocation> location(@NotNull Object nativeLocation);
}
