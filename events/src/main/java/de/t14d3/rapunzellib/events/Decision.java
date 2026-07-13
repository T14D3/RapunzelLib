package de.t14d3.rapunzellib.events;

/**
 * Decision outcome for cancellable events.
 *
 * <p>PASS - Event passes to next handler without making a decision.
 * The next handler or default behavior will determine the outcome.
 *
 * <p>ALLOW - Explicitly allow the action to proceed, overriding any
 * default behavior or other handlers.
 *
 * <p>DENY - Deny the action with an optional reason component.
 * The action will be cancelled and the reason may be shown to the player.
 */
public enum Decision {
    PASS,
    ALLOW,
    DENY
}
