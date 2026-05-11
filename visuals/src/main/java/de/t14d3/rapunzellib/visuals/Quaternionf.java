package de.t14d3.rapunzellib.visuals;

public record Quaternionf(float x, float y, float z, float w) {
    public Quaternionf() {
        this(0, 0, 0, 1);
    }
}
