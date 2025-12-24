package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RWorld extends RNative {
    RWorldRef ref();

    default Optional<UUID> uuid() {
        return Optional.empty();
    }

    static Collection<RWorld> all() {
        return Rapunzel.worlds().all();
    }

    static Optional<RWorld> getByName(String name) {
        return Rapunzel.worlds().getByName(name);
    }

    static Optional<RWorld> wrap(Object nativeWorld) {
        return Rapunzel.worlds().wrap(nativeWorld);
    }
}

