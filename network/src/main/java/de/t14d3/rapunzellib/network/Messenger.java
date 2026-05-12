package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;

/**
 * Abstraction for sending and receiving messages across a server network.
 */
public interface Messenger {
    /**
     * Sends a message to all servers on this network transport.
     *
     * <p>Transports may impose delivery constraints (e.g. plugin messaging often
     * requires a connected player as the carrier). Implementations should not
     * throw on transient delivery constraints; prefer making {@link #isConnected()}
     * accurately reflect whether delivery is currently possible.</p>
     *
     * @param channel the message channel
     * @param data the message payload
     */
    void sendToAll(@NotNull String channel, @NotNull String data);

    /**
     * Sends a message to a specific server.
     *
     * <p>Targeting is best-effort depending on transport.</p>
     *
     * @param channel the message channel
     * @param serverName the target server name
     * @param data the message payload
     */
    void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data);

    /**
     * Sends a message to the proxy side of the network (e.g. Velocity).
     *
     * <p>Targeting is best-effort depending on transport.</p>
     *
     * @param channel the message channel
     * @param data the message payload
     */
    void sendToProxy(@NotNull String channel, @NotNull String data);

    /**
     * Registers a listener for messages on the given channel.
     *
     * @param channel the channel to listen on
     * @param listener the listener to register
     */
    void registerListener(@NotNull String channel, @NotNull MessageListener listener);

    /**
     * Unregisters a listener from the given channel.
     *
     * @param channel the channel to remove from
     * @param listener the listener to unregister
     */
    void unregisterListener(@NotNull String channel, @NotNull MessageListener listener);

    /**
     * Whether this messenger can currently deliver messages.
     *
     * <p>Note: "connected" is transport-specific. For plugin-messaging-based
     * transports this may depend on an online player being available as carrier.</p>
     *
     * @return true if messages can be delivered
     */
    boolean isConnected();

    /**
     * Returns the name of this server.
     *
     * @return the server name
     */
    @NotNull String getServerName();

    /**
     * Returns the name of the proxy server.
     *
     * @return the proxy server name
     */
    @NotNull String getProxyServerName();
}

