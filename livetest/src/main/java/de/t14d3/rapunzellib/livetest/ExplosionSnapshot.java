package de.t14d3.rapunzellib.livetest;

/**
 * Immutable snapshot of an explosion event received from the server.
 */
public record ExplosionSnapshot(double x, double y, double z, float radius) {}