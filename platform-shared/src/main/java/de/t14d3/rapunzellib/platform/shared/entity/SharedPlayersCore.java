package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.common.objects.AbstractPlayerStore;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstract base implementation of a player store backed by Minecraft's {@link MinecraftServer#getPlayerList()}.
 * <p>
 * Provides online player enumeration, UUID-based lookup, native player adaptation,
 * and player wrapping via the {@link AbstractPlayerStore} infrastructure.
 * </p>
 *
 * @param <P> the concrete server player wrapper type
 */
public abstract class SharedPlayersCore<P extends RServerPlayer> extends AbstractPlayerStore<ServerPlayer, P> {
    private final MinecraftServer server;

    /**
     * Constructs a new players core.
     *
     * @param server the Minecraft server instance
     */
    protected SharedPlayersCore(@NotNull MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull Collection<? extends ServerPlayer> nativeOnlinePlayers() {
        return server.getPlayerList().getPlayers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull Optional<? extends ServerPlayer> findNativePlayer(@NotNull UUID uuid) {
        return Optional.ofNullable(server.getPlayerList().getPlayer(uuid));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull Optional<? extends ServerPlayer> adaptNativePlayer(@NotNull Object nativePlayer) {
        return nativePlayer instanceof ServerPlayer player ? Optional.of(player) : Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull UUID playerId(@NotNull ServerPlayer nativePlayer) {
        return nativePlayer.getUUID();
    }

    /**
     * Wraps a native ServerPlayer into the managed player wrapper type.
     *
     * @param player the native server player
     * @return an Optional containing the wrapped player
     */
    public final @NotNull Optional<P> wrap(@NotNull ServerPlayer player) {
        return wrapNative(player);
    }

    /**
     * Gets or creates the managed wrapper for the given native server player.
     *
     * @param player the native server player
     * @return the wrapped player instance
     */
    protected final @NotNull P requireServer(@NotNull ServerPlayer player) {
        return wrapPlayer(player);
    }
}
