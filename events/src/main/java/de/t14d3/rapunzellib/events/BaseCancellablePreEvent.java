package de.t14d3.rapunzellib.events;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Abstract base implementation of {@link CancellablePreEvent}.
 *
 * <p>Provides thread-safe decision state management using volatile fields.
 * Subclasses construct the event with the initial cancelled state and
 * expose event-specific data through their own getters.</p>
 */
public abstract class BaseCancellablePreEvent implements CancellablePreEvent {
    /**
     * The current decision for this event.
     */
    private volatile Decision decision = Decision.PASS;
    /**
     * Optional reason for denial.
     */
    private volatile Component denyReason;
    /**
     * Whether the original platform event was cancelled before dispatch.
     */
    private volatile boolean cancelled;

    /**
     * Returns the current decision for this event.
     *
     * @return the decision
     */
    @Override
    public final Decision decision() {
        return decision;
    }

    /**
     * Returns whether the platform event was already cancelled.
     *
     * @return true if the original event was cancelled
     */
    @Override
    public final boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancelled state. For use by subclasses during construction.
     *
     * @param cancelled the cancelled state to set
     */
    protected final void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Passes the event to the next handler.
     */
    @Override
    public final void pass() {
        decision = Decision.PASS;
        denyReason = null;
    }

    /**
     * Explicitly allows the action to proceed (only if not already denied).
     */
    @Override
    public final void allow() {
        // ALLOW currently behaves like PASS for platform bridges; it exists mainly for
        // future override/priority semantics.
        if (decision != Decision.DENY) {
            decision = Decision.ALLOW;
            denyReason = null;
        }
    }

    /**
     * Denies the action without a reason.
     */
    @Override
    public final void deny() {
        denyReason = null;
        decision = Decision.DENY;
    }

    /**
     * Denies the action with a reason.
     *
     * @param reason the reason for denial
     */
    @Override
    public final void deny(Component reason) {
        denyReason = Objects.requireNonNull(reason, "reason");
        decision = Decision.DENY;
    }

    /**
     * Returns the optional deny reason.
     *
     * @return an optional containing the deny reason, if set
     */
    @Override
    public final @NotNull Optional<Component> denyReason() {
        return Optional.ofNullable(denyReason);
    }
}
