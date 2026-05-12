package de.t14d3.rapunzellib.common.objects;

import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstract base for player stores that wrap native player objects into {@link RPlayer} instances.
 * <p>
 * Provides cached wrapping via {@link CachedWrapperStore} and default implementations
 * for the {@link Players} interface methods. Subclasses need only implement native
 * player lookup and ID extraction.
 *
 * @param <N> the native player handle type
 * @param <W> the wrapper player type
 */
public abstract class AbstractPlayerStore<N, W extends RPlayer> extends CachedWrapperStore<UUID, N, W> implements Players {
    /**
     * Returns all online players wrapped as {@link RPlayer} instances.
     *
     * @return a collection of online players
     */
    @Override
    public final @NotNull Collection<RPlayer> online() {
        return nativeOnlinePlayers().stream().map(this::wrapPlayer).map(RPlayer.class::cast).toList();
    }

    /**
     * Looks up a player by UUID.
     *
     * @param uuid the player UUID
     * @return an optional containing the player, or empty if not found
     */
    @Override
    public final @NotNull Optional<RPlayer> get(@NotNull UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return findNativePlayer(uuid).map(this::wrapPlayer).map(RPlayer.class::cast);
    }

    /**
     * Wraps a native player object into an {@link RPlayer}.
     *
     * @param nativePlayer the native player object
     * @return an optional containing the wrapped player
     */
    @Override
    public final @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
        Objects.requireNonNull(nativePlayer, "nativePlayer");
        return adaptNativePlayer(nativePlayer).flatMap(this::wrapNative).map(RPlayer.class::cast);
    }

    /**
     * Returns all online native player handles.
     *
     * @return a collection of native player handles
     */
    protected abstract @NotNull Collection<? extends N> nativeOnlinePlayers();

    /**
     * Finds a native player handle by UUID.
     *
     * @param uuid the player UUID
     * @return an optional containing the native handle, or empty if not found
     */
    protected abstract @NotNull Optional<? extends N> findNativePlayer(@NotNull UUID uuid);

    /**
     * Attempts to adapt a generic native object into a native player handle.
     *
     * @param nativePlayer the native object to adapt
     * @return an optional containing the adapted handle, or empty if not compatible
     */
    protected abstract @NotNull Optional<? extends N> adaptNativePlayer(@NotNull Object nativePlayer);

    /**
     * Extracts the UUID from a native player handle.
     *
     * @param nativePlayer the native player handle
     * @return the player UUID
     */
    protected abstract @NotNull UUID playerId(@NotNull N nativePlayer);

    /**
     * Wraps a native player handle into the cached wrapper type.
     *
     * @param nativePlayer the native player handle
     * @return an optional containing the wrapped player
     */
    public final @NotNull Optional<W> wrapNative(@NotNull N nativePlayer) {
        return Optional.of(wrapPlayer(nativePlayer));
    }

    /**
     * Wraps a native player handle, using the cache if available.
     *
     * @param nativePlayer the native player handle
     * @return the wrapped player
     */
    protected final @NotNull W wrapPlayer(@NotNull N nativePlayer) {
        Objects.requireNonNull(nativePlayer, "nativePlayer");
        return wrapCached(playerId(nativePlayer), nativePlayer);
    }
}
