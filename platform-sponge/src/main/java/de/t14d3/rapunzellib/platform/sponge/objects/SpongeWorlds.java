package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.Worlds;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Collection;
import java.util.Optional;

public final class SpongeWorlds implements Worlds {
    @Override
    public @NotNull Collection<RWorld> all() {
        if (!Sponge.isServerAvailable()) return java.util.List.of();
        return Sponge.server().worldManager().worlds().stream()
            .map(SpongeWorld::new)
            .map(RWorld.class::cast)
            .toList();
    }

    @Override
    public @NotNull Optional<RWorld> getByName(@NotNull String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        if (!Sponge.isServerAvailable()) return Optional.empty();
        String target = name.trim();
        return Sponge.server().worldManager().worlds().stream()
            .filter(w -> w.properties().name().equalsIgnoreCase(target))
            .findFirst()
            .map(SpongeWorld::new)
            .map(RWorld.class::cast);
    }

    @Override
    public @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        if (!(nativeWorld instanceof ServerWorld world)) return Optional.empty();
        return Optional.of(new SpongeWorld(world));
    }
}
