package de.t14d3.rapunzellib.events;

import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.Optional;

public abstract class BaseCancellablePreEvent implements CancellablePreEvent {
    private volatile Decision decision = Decision.PASS;
    private volatile Component denyReason;
    protected volatile boolean cancelled;

    @Override
    public final Decision decision() {
        return decision;
    }

    @Override
    public final boolean isCancelled() {
        return cancelled;
    }

    @Override
    public final void pass() {
        decision = Decision.PASS;
        denyReason = null;
    }

    @Override
    public final void allow() {
        // ALLOW currently behaves like PASS for platform bridges; it exists mainly for
        // future override/priority semantics.
        if (decision != Decision.DENY) {
            decision = Decision.ALLOW;
            denyReason = null;
        }
    }

    @Override
    public final void deny() {
        denyReason = null;
        decision = Decision.DENY;
    }

    @Override
    public final void deny(Component reason) {
        denyReason = Objects.requireNonNull(reason, "reason");
        decision = Decision.DENY;
    }

    @Override
    public final Optional<Component> denyReason() {
        return Optional.ofNullable(denyReason);
    }
}
