package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PaperPlayers implements Players {
    private final ConcurrentHashMap<UUID, PaperPlayer> cache = new ConcurrentHashMap<>();

    @Override
    public Collection<RPlayer> online() {
        return Bukkit.getOnlinePlayers().stream().map(this::wrapInternal).map(RPlayer.class::cast).toList();
    }

    @Override
    public Optional<RPlayer> get(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return Optional.empty();
        return Optional.of(wrapInternal(player));
    }

    @Override
    public Optional<RPlayer> wrap(Object nativePlayer) {
        if (!(nativePlayer instanceof Player player)) return Optional.empty();
        return Optional.of(wrapInternal(player));
    }

    private PaperPlayer wrapInternal(Player player) {
        return cache.compute(player.getUniqueId(), (uuid, existing) -> {
            if (existing == null) return new PaperPlayer(player);
            existing.updateHandle(player);
            return existing;
        });
    }
}

