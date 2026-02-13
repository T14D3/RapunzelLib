package de.t14d3.rapunzellib.network.outbox;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface NetworkOutboxPolicy {
    @NotNull NetworkDeliverySemantics semantics(@NotNull NetworkSendRequest request);

    default boolean shouldStore(@NotNull NetworkSendRequest request) {
        Objects.requireNonNull(request, "request");
        return semantics(request) == NetworkDeliverySemantics.STORE_AND_FORWARD;
    }

    static @NotNull NetworkOutboxPolicy directOnly() {
        return request -> NetworkDeliverySemantics.DIRECT_ONLY;
    }
}
