package de.t14d3.rapunzellib.events;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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

    public boolean supported() {
        return parity != GameEventSupportParity.UNSUPPORTED;
    }

    public static @NotNull GameEventSupport unsupported(@NotNull Class<? extends GameEvent> eventType) {
        return new GameEventSupport(eventType, GameEventSupportParity.UNSUPPORTED, "");
    }
}
