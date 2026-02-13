package de.t14d3.rapunzellib.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BootstrapHandle implements AutoCloseable {
    private final Object participant;
    private final RapunzelContext context;
    private final BootstrapOwnerRole role;
    private final Runnable closeAction;
    private final AtomicBoolean closed = new AtomicBoolean();

    public BootstrapHandle(
        @NotNull Object participant,
        @NotNull RapunzelContext context,
        @NotNull BootstrapOwnerRole role,
        @NotNull Runnable closeAction
    ) {
        this.participant = Objects.requireNonNull(participant, "participant");
        this.context = Objects.requireNonNull(context, "context");
        this.role = Objects.requireNonNull(role, "role");
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
    }

    public @NotNull Object participant() {
        return participant;
    }

    public @NotNull RapunzelContext context() {
        return context;
    }

    public @NotNull BootstrapOwnerRole role() {
        return role;
    }

    public boolean isOwner() {
        return role == BootstrapOwnerRole.OWNER;
    }

    public boolean isBorrower() {
        return role == BootstrapOwnerRole.BORROWER;
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        closeAction.run();
    }
}
