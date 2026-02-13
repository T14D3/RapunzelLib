package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RServerPlayer extends RPlayer, RLivingEntity {
    @Override
    default @NotNull RRegistryRef<REntityType> typeRef() {
        return REntityType.ref("minecraft:player");
    }

    @Override
    default boolean isLivingEntity() {
        return true;
    }

    @Override
    default @NotNull Optional<RLivingEntity> asLivingEntity() {
        return Optional.of(this);
    }

    @Override
    default @NotNull RLivingEntity requireLivingEntity() {
        return this;
    }

    @Override
    @NotNull Optional<RWorld> world();

    @Override
    @NotNull Optional<RLocation> location();

    default @NotNull RWorld worldOrThrow() {
        return world().orElseThrow(() -> new UnsupportedOperationException("world is not supported for " + getClass().getName()));
    }

    default @NotNull RLocation locationOrThrow() {
        return location().orElseThrow(() -> new UnsupportedOperationException("location is not supported for " + getClass().getName()));
    }

}
