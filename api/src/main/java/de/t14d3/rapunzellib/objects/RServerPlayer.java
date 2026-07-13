package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A server-side connected player that is also a living entity in the world.
 *
 * <p>This interface combines player and living-entity semantics for players
 * that are actively connected to a game server (as opposed to proxy-only players).</p>
 */
public interface RServerPlayer extends RPlayer, RLivingEntity {
    @Override
    default @NotNull RRegistryRef<REntityType> typeRef() {
        return REntityType.ref("minecraft:player");
    }

    @Override
    default boolean isLivingEntity() {
        return true;
    }

    @Override
    default @NotNull Optional<RLivingEntity> asLivingEntity() {
        return Optional.of(this);
    }

    @Override
    default @NotNull RLivingEntity requireLivingEntity() {
        return this;
    }

    /**
     * Returns the world this server player is in, if available.
     *
     * @return an {@link Optional} containing the world, or empty if unknown
     */
    @Override
    @NotNull Optional<RWorld> world();

    /**
     * Returns the location of this server player, if available.
     *
     * @return an {@link Optional} containing the location, or empty if unknown
     */
    @Override
    @NotNull Optional<RLocation> location();

    /**
     * Returns the world this server player is in, throwing if unavailable.
     *
     * @return the world
     * @throws UnsupportedOperationException if the world is not available
     */
    default @NotNull RWorld worldOrThrow() {
        return world().orElseThrow(() -> new UnsupportedOperationException("world is not supported for " + getClass().getName()));
    }

    /**
     * Returns the location of this server player, throwing if unavailable.
     *
     * @return the location
     * @throws UnsupportedOperationException if the location is not available
     */
    default @NotNull RLocation locationOrThrow() {
        return location().orElseThrow(() -> new UnsupportedOperationException("location is not supported for " + getClass().getName()));
    }

    /**
     * Connects this player to a different server on the network and teleports them
     * to the specified location on arrival.
     *
     * <p>Requires a proxy (e.g. Velocity) and the RapunzelLib network module
     * to be installed on all servers involved.</p>
     *
     * <p>Default implementation throws {@link UnsupportedOperationException}.
     * Platform implementations (including {@code RemotePlayer}) should override this.</p>
     *
     * @param serverName the target server name (as configured in the proxy)
     * @param location   the location to teleport to on the target server
     * @return a {@link CompletableFuture} that completes with {@code true} if the
     *         player was successfully moved and queued for teleport, {@code false} otherwise
     */
    default @NotNull CompletableFuture<Boolean> connectToServer(@NotNull String serverName, @NotNull RLocation location) {
        throw new UnsupportedOperationException("connectToServer is not supported for " + getClass().getName());
    }

    /**
     * Connects this player to a different server on the network without a specific location.
     *
     * <p>Default implementation throws {@link UnsupportedOperationException}.</p>
     *
     * @param serverName the target server name
     * @return a {@link CompletableFuture} that completes with {@code true} if the
     *         player was successfully moved
     */
    default @NotNull CompletableFuture<Boolean> connectToServer(@NotNull String serverName) {
        throw new UnsupportedOperationException("connectToServer is not supported for " + getClass().getName());
    }

    /**
     * Sets the game mode for this player.
     *
     * @param gameMode the game mode to set
     * @throws UnsupportedOperationException if not supported by this implementation
     */
    default void gameMode(@NotNull RGameMode gameMode) {
        throw new UnsupportedOperationException("gameMode(RGameMode) is not supported for " + getClass().getName());
    }

    /**
     * Returns the current game mode of this player.
     *
     * @return the current game mode
     * @throws UnsupportedOperationException if not supported by this implementation
     */
    default @NotNull RGameMode gameMode() {
        throw new UnsupportedOperationException("gameMode() is not supported for " + getClass().getName());
    }

    /**
     * Sets the operator status for this player.
     *
     * @param op {@code true} to op, {@code false} to deop
     * @throws UnsupportedOperationException if not supported by this implementation
     */
    default void op(boolean op) {
        throw new UnsupportedOperationException("op(boolean) is not supported for " + getClass().getName());
    }

    /**
     * Checks whether this player is an operator.
     *
     * @return {@code true} if the player is an operator
     * @throws UnsupportedOperationException if not supported by this implementation
     */
    default boolean op() {
        throw new UnsupportedOperationException("op() is not supported for " + getClass().getName());
    }
}
