package de.t14d3.rapunzellib.livetest;

/**
 * Immutable snapshot of a block change event received from the server.
 * Each snapshot captures the world position and the new block state id
 * at that position as reported by a {@code ClientboundBlockUpdatePacket}
 * or a {@code ClientboundSectionBlocksUpdatePacket}.
 */
public record BlockSnapshot(int x, int y, int z, int blockStateId) {}