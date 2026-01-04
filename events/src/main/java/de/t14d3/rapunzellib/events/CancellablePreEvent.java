package de.t14d3.rapunzellib.events;

import net.kyori.adventure.text.Component;

import java.util.Optional;

public interface CancellablePreEvent extends GamePreEvent {
    Decision decision();

    /**
     * Whether the platform event was already cancelled before this event was dispatched.
     *
     * <p>This allows consumers to still react to cancelled events while being able to tell
     * if cancellation came from another plugin/mod.</p>
     */
    boolean isCancelled();

    default boolean isDenied() {
        return decision() == Decision.DENY;
    }

    default boolean isAllowed() {
        return decision() == Decision.ALLOW;
    }

    void pass();

    void allow();

    void deny();

    void deny(Component reason);

    Optional<Component> denyReason();
}
