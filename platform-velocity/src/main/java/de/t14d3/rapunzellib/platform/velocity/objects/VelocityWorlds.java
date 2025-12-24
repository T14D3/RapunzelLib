package de.t14d3.rapunzellib.platform.velocity.objects;

import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.Worlds;

import java.util.Collection;
import java.util.Optional;

public final class VelocityWorlds implements Worlds {
    @Override
    public Collection<RWorld> all() {
        return java.util.List.of();
    }

    @Override
    public Optional<RWorld> getByName(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<RWorld> wrap(Object nativeWorld) {
        return Optional.empty();
    }
}

