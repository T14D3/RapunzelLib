package de.t14d3.rapunzellib.network.runtime;

/**
 * Types of network links between servers in the Rapunzel network.
 *
 * <p>Each link kind represents a different transport mechanism for
 * inter-server communication.
 */
public enum NetworkLinkKind {
    /** In-memory communication (same JVM, no external transport). */
    IN_MEMORY,
    /** Minecraft plugin messaging channels (Velocity/BungeeCord). */
    PLUGIN_MESSAGING,
    /** Redis pub/sub messaging. */
    REDIS_PUBSUB,
    /** Direct TCP socket RPC communication. */
    RPC
}
