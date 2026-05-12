package de.t14d3.rapunzellib.events;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Describes the level of support for a specific event type on a particular platform.
 *
 * <p>A record containing the event type, its {@link GameEventSupportParity parity level},
 * and a human-readable details string explaining the nature of support.</p>
 *
 * @param eventType the event class
 * @param parity    the support parity level
 * @param details   human-readable description of the support status
 */
public record GameEventSupport(
    @NotNull Class<? extends GameEvent> eventType,
    @NotNull GameEventSupportParity parity,
    @NotNull String details
) {
    public GameEventSupport {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(parity, "parity");
        Objects.requireNonNull(details, "details");
    }

    /**
     * Returns whether this event type is supported (not {@link GameEventSupportParity#UNSUPPORTED}).
     *
     * @return true if supported
     */
    public boolean supported() {
        return parity != GameEventSupportParity.UNSUPPORTED;
    }

    /**
     * Creates an unsupported support entry for the given event type.
     *
     * @param eventType the event class
     * @return an unsupported GameEventSupport
     */
    public static @NotNull GameEventSupport unsupported(@NotNull Class<? extends GameEvent> eventType) {
        return new GameEventSupport(eventType, GameEventSupportParity.UNSUPPORTED, "");
    }
}
