package de.t14d3.rapunzellib.runtime;

/**
 * The engine family that the platform is based on.
 */
public enum EngineFamily {
    /** Standard Mojang-mapped Minecraft server (Paper, Fabric, NeoForge). */
    MOJANG_SERVER,
    /** Sponge-powered server. */
    SPONGE_SERVER,
    /** Proxy platform (Velocity). */
    PROXY
}
