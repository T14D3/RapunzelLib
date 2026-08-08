package de.t14d3.rapunzellib.network.rpcserver;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Optional routing hooks for the RPC server side of the transport.
 *
 * <p>These hooks let the proxy (Velocity) extend the TCP bridge routing with
 * backends that are known to the network but are <em>not</em> connected over
 * the TCP bridge (e.g. they only have a plugin-messaging carrier). Without
 * them, envelopes addressed to such backends would be silently dropped.</p>
 *
 * @param knownBackends   supplier of all backend server names known to the network
 * @param externalForward fallback used when a backend is known but not TCP-connected
 */
public record RoutingHooks(
    Supplier<Collection<String>> knownBackends,
    ExternalForward externalForward
) {
    public RoutingHooks {
        Objects.requireNonNull(knownBackends, "knownBackends");
        Objects.requireNonNull(externalForward, "externalForward");
    }

    /**
     * Attempts to deliver an envelope to a backend that is not connected over TCP.
     *
     * @param channel      the message channel
     * @param data         the serialized payload
     * @param sourceServer the originating server name
     * @param targetServer the backend to deliver to
     * @return true if the envelope was handed off successfully
     */
    @FunctionalInterface
    public interface ExternalForward {
        boolean forward(String channel, String data, String sourceServer, String targetServer);
    }

    /** No-op hooks: routing falls back to TCP-connected clients only. */
    public static final RoutingHooks NONE = new RoutingHooks(List::of, (c, d, s, t) -> false);
}
