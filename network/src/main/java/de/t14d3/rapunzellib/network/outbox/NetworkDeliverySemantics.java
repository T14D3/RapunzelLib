package de.t14d3.rapunzellib.network.outbox;

/**
 * Delivery semantics for network messages.
 */
public enum NetworkDeliverySemantics {
    /** Deliver immediately; no storage or retry. */
    DIRECT_ONLY,
    /** Store messages temporarily and forward when connection is available. */
    STORE_AND_FORWARD
}
