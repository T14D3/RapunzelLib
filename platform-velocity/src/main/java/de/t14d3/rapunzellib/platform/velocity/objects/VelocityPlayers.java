package de.t14d3.rapunzellib.platform.velocity.objects;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocityPlayers implements Players {
    private final ProxyServer proxy;
    private final ConcurrentHashMap<UUID, VelocityPlayer> cache = new ConcurrentHashMap<>();

    public VelocityPlayers(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public Collection<RPlayer> online() {
        return proxy.getAllPlayers().stream().map(this::wrapInternal).map(RPlayer.class::cast).toList();
    }

    @Override
    public Optional<RPlayer> get(UUID uuid) {
        return proxy.getPlayer(uuid).map(this::wrapInternal);
    }

    @Override
    public Optional<RPlayer> wrap(Object nativePlayer) {
        if (!(nativePlayer instanceof Player player)) return Optional.empty();
        return Optional.of(wrapInternal(player));
    }

    private VelocityPlayer wrapInternal(Player player) {
        return cache.compute(player.getUniqueId(), (uuid, existing) -> {
            if (existing == null) return new VelocityPlayer(player);
            existing.updateHandle(player);
            return existing;
        });
    }
}

