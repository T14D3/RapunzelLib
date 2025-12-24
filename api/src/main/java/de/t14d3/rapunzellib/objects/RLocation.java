package de.t14d3.rapunzellib.objects;

public record RLocation(RWorldRef world, double x, double y, double z, float yaw, float pitch) {
    public RBlockPos blockPos() {
        return new RBlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}

