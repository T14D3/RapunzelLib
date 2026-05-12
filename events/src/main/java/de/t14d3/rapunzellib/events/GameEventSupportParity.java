package de.t14d3.rapunzellib.events;

/**
 * Parity levels indicating how an event type is supported on a given platform.
 *
 * <p>NATIVE - The platform natively supports this event type.
 * EMULATED - The event type is emulated via other platform equivalents.
 * PARTIAL - Only partial support is available (e.g., some scenarios are not covered).
 * UNSUPPORTED - The event type is not available on this platform.</p>
 */
public enum GameEventSupportParity {
    NATIVE,
    EMULATED,
    PARTIAL,
    UNSUPPORTED
}
