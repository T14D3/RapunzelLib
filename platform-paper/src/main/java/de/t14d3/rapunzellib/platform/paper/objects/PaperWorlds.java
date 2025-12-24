package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.Worlds;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PaperWorlds implements Worlds {
    private final ConcurrentHashMap<UUID, PaperWorld> cache = new ConcurrentHashMap<>();

    @Override
    public Collection<RWorld> all() {
        return Bukkit.getWorlds().stream().map(this::wrapInternal).map(RWorld.class::cast).toList();
    }

    @Override
    public Optional<RWorld> getByName(String name) {
        World world = Bukkit.getWorld(name);
        return (world != null) ? Optional.of(wrapInternal(world)) : Optional.empty();
    }

    @Override
    public Optional<RWorld> wrap(Object nativeWorld) {
        if (!(nativeWorld instanceof World world)) return Optional.empty();     
        return Optional.of(wrapInternal(world));
    }

    private PaperWorld wrapInternal(World world) {
        return cache.compute(world.getUID(), (uuid, existing) -> {
            if (existing == null) return new PaperWorld(world);
            existing.updateHandle(world);
            return existing;
        });
    }
}

