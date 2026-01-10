package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgePlayers implements Players {
    private final MinecraftServer server;
    private final ConcurrentHashMap<UUID, NeoForgePlayer> cache = new ConcurrentHashMap<>();

    public NeoForgePlayers(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public @NotNull Collection<RPlayer> online() {
        return server.getPlayerList().getPlayers().stream()
            .map(this::wrapInternal)
            .map(RPlayer.class::cast)
            .toList();
    }

    @Override
    public @NotNull Optional<RPlayer> get(@NotNull UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return Optional.empty();
        return Optional.of(wrapInternal(player));
    }

    @Override
    public @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
        if (!(nativePlayer instanceof ServerPlayer player)) return Optional.empty();
        return Optional.of(wrapInternal(player));
    }

    private NeoForgePlayer wrapInternal(ServerPlayer player) {
        return cache.compute(player.getUUID(), (uuid, existing) -> {
            if (existing == null) return new NeoForgePlayer(player);
            existing.updateHandle(player);
            return existing;
        });
    }
}
