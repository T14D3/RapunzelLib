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
    @Override
    public final @NotNull Collection<RPlayer> online() {
        return nativeOnlinePlayers().stream().map(this::wrapPlayer).map(RPlayer.class::cast).toList();
    }

    @Override
    public final @NotNull Optional<RPlayer> get(@NotNull UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return findNativePlayer(uuid).map(this::wrapPlayer).map(RPlayer.class::cast);
    }

    @Override
    public final @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
        Objects.requireNonNull(nativePlayer, "nativePlayer");
        return adaptNativePlayer(nativePlayer).flatMap(this::wrapNative).map(RPlayer.class::cast);
    }

    protected abstract @NotNull Collection<? extends N> nativeOnlinePlayers();

    protected abstract @NotNull Optional<? extends N> findNativePlayer(@NotNull UUID uuid);

    protected abstract @NotNull Optional<? extends N> adaptNativePlayer(@NotNull Object nativePlayer);

    protected abstract @NotNull UUID playerId(@NotNull N nativePlayer);

    public final @NotNull Optional<W> wrapNative(@NotNull N nativePlayer) {
        return Optional.of(wrapPlayer(nativePlayer));
    }

    protected final @NotNull W wrapPlayer(@NotNull N nativePlayer) {
        Objects.requireNonNull(nativePlayer, "nativePlayer");
        return wrapCached(playerId(nativePlayer), nativePlayer);
    }
}
