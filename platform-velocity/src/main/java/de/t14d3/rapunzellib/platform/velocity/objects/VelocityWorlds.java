package de.t14d3.rapunzellib.platform.velocity.objects;

import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.Worlds;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public final class VelocityWorlds implements Worlds {
    @Override
    public @NotNull Collection<RWorld> all() {
        return java.util.List.of();
    }

    @Override
    public @NotNull Optional<RWorld> getByName(@NotNull String name) {
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        return Optional.empty();
    }
}

