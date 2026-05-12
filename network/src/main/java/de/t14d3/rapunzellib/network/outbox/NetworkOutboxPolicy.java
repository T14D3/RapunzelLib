package de.t14d3.rapunzellib.network.outbox;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Policy interface for determining delivery semantics of network requests.
 *
 * <p>Implementations decide whether a message should be delivered directly or
 * stored-and-forwarded based on the request properties.
 */
@FunctionalInterface
public interface NetworkOutboxPolicy {
    /**
     * Determines the delivery semantics for the given request.
     *
     * @param request the network send request
     * @return the delivery semantics
     */
    @NotNull NetworkDeliverySemantics semantics(@NotNull NetworkSendRequest request);

    /**
     * Returns whether the request should be stored-and-forwarded.
     *
     * @param request the network send request
     * @return true if the message should be stored
     */
    default boolean shouldStore(@NotNull NetworkSendRequest request) {
        Objects.requireNonNull(request, "request");
        return semantics(request) == NetworkDeliverySemantics.STORE_AND_FORWARD;
    }

    /**
     * Returns a policy that treats all requests as DIRECT_ONLY.
     *
     * @return a direct-only policy
     */
    static @NotNull NetworkOutboxPolicy directOnly() {
        return request -> NetworkDeliverySemantics.DIRECT_ONLY;
    }
}
