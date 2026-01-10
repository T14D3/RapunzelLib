package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RWorld extends RNative {
    @NotNull RWorldRef ref();

    default @NotNull Optional<UUID> uuid() {
        return Optional.empty();
    }

    static @NotNull Collection<RWorld> all() {
        return Rapunzel.worlds().all();
    }

    static @NotNull Optional<RWorld> getByName(@NotNull String name) {
        return Rapunzel.worlds().getByName(name);
    }

    static @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        return Rapunzel.worlds().wrap(nativeWorld);
    }
}

