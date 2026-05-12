package de.t14d3.rapunzellib.network.runtime;

/**
 * Roles a node can assume in the Rapunzel network topology.
 */
public enum NetworkNodeRole {
    /** The proxy/velocity server that manages backend connections. */
    PROXY,
    /** A backend Minecraft server (Paper, Sponge, etc.). */
    BACKEND
}
