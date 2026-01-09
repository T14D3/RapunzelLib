package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpongePlayers implements Players {
    private final ConcurrentHashMap<UUID, SpongePlayer> cache = new ConcurrentHashMap<>();

    @Override
    public Collection<RPlayer> online() {
        if (!Sponge.isServerAvailable()) return java.util.List.of();
        return Sponge.server().onlinePlayers().stream()
            .map(this::wrapInternal)
            .map(RPlayer.class::cast)
            .toList();
    }

    @Override
    public Optional<RPlayer> get(UUID uuid) {
        if (uuid == null) return Optional.empty();
        if (!Sponge.isServerAvailable()) return Optional.empty();
        return Sponge.server().player(uuid).map(this::wrapInternal).map(RPlayer.class::cast);
    }

    @Override
    public Optional<RPlayer> wrap(Object nativePlayer) {
        if (!(nativePlayer instanceof ServerPlayer player)) return Optional.empty();
        return Optional.of(wrapInternal(player));
    }

    private SpongePlayer wrapInternal(ServerPlayer nativePlayer) {
        UUID uuid = nativePlayer.profile().uuid();
        return cache.compute(uuid, (ignored, existing) -> {
            if (existing == null) return new SpongePlayer(nativePlayer);
            existing.updateHandle(nativePlayer);
            return existing;
        });
    }
}
