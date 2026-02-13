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

public abstract class SharedPlayersCore<P extends RServerPlayer> extends AbstractPlayerStore<ServerPlayer, P> {
    private final MinecraftServer server;

    protected SharedPlayersCore(@NotNull MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    protected @NotNull Collection<? extends ServerPlayer> nativeOnlinePlayers() {
        return server.getPlayerList().getPlayers();
    }

    @Override
    protected @NotNull Optional<? extends ServerPlayer> findNativePlayer(@NotNull UUID uuid) {
        return Optional.ofNullable(server.getPlayerList().getPlayer(uuid));
    }

    @Override
    protected @NotNull Optional<? extends ServerPlayer> adaptNativePlayer(@NotNull Object nativePlayer) {
        return nativePlayer instanceof ServerPlayer player ? Optional.of(player) : Optional.empty();
    }

    @Override
    protected @NotNull UUID playerId(@NotNull ServerPlayer nativePlayer) {
        return nativePlayer.getUUID();
    }

    public final @NotNull Optional<P> wrap(@NotNull ServerPlayer player) {
        return wrapNative(player);
    }

    protected final @NotNull P requireServer(@NotNull ServerPlayer player) {
        return wrapPlayer(player);
    }
}
