package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

public record RLocation(@NotNull RWorldRef world, double x, double y, double z, float yaw, float pitch) {
    public @NotNull RBlockPos blockPos() {
        return new RBlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}

