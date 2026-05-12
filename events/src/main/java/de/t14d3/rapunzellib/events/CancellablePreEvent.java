package de.t14d3.rapunzellib.events;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Interface for pre-events that support cancellation and decision-making.
 *
 * <p>Implementations allow handlers to examine the original platform cancelled state,
 * vote via {@link Decision} (PASS/ALLOW/DENY), and provide an optional deny reason.</p>
 */
public interface CancellablePreEvent extends GamePreEvent {
    /**
     * Returns the current decision for this event.
     *
     * @return the decision
     */
    Decision decision();

    /**
     * Whether the platform event was already cancelled before this event was dispatched.
     *
     * <p>This allows consumers to still react to cancelled events while being able to tell
     * if cancellation came from another plugin/mod.</p>
     */
    boolean isCancelled();

    /**
     * Returns whether this event has been denied.
     *
     * @return true if the decision is DENY
     */
    default boolean isDenied() {
        return decision() == Decision.DENY;
    }

    /**
     * Returns whether this event has been explicitly allowed.
     *
     * @return true if the decision is ALLOW
     */
    default boolean isAllowed() {
        return decision() == Decision.ALLOW;
    }

    /**
     * Passes the event to the next handler without making a decision.
     */
    void pass();

    /**
     * Explicitly allows the action to proceed.
     */
    void allow();

    /**
     * Denies the action without a reason.
     */
    void deny();

    /**
     * Denies the action with an optional reason shown to the player.
     *
     * @param reason the reason for denial, may be null
     */
    void deny(@Nullable Component reason);

    /**
     * Returns the reason for denial, if set.
     *
     * @return an optional containing the deny reason
     */
    @NotNull Optional<Component> denyReason();
}
