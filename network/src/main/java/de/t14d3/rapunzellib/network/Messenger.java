package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;

public interface Messenger {
    /**
     * Sends a message to all servers on this network transport.
     *
     * <p>Transports may impose delivery constraints (e.g. plugin messaging often
     * requires a connected player as the carrier). Implementations should not
     * throw on transient delivery constraints; prefer making {@link #isConnected()}
     * accurately reflect whether delivery is currently possible.</p>
     */
    void sendToAll(@NotNull String channel, @NotNull String data);

    /**
     * Sends a message to a specific server.
     *
     * <p>Targeting is best-effort depending on transport.</p>
     */
    void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data);

    /**
     * Sends a message to the proxy side of the network (e.g. Velocity).
     *
     * <p>Targeting is best-effort depending on transport.</p>
     */
    void sendToProxy(@NotNull String channel, @NotNull String data);

    void registerListener(@NotNull String channel, @NotNull MessageListener listener);

    void unregisterListener(@NotNull String channel, @NotNull MessageListener listener);

    /**
     * Whether this messenger can currently deliver messages.
     *
     * <p>Note: "connected" is transport-specific. For plugin-messaging-based
     * transports this may depend on an online player being available as carrier.</p>
     */
    boolean isConnected();

    @NotNull String getServerName();

    @NotNull String getProxyServerName();
}

