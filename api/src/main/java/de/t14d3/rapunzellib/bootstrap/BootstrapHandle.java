package de.t14d3.rapunzellib.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A handle representing a plugin or module's participation in a RapunzelLib context lifecycle.
 *
 * <p>Owners create contexts via bootstrapping, while borrowers acquire a reference to an
 * existing context. Closing the handle triggers shutdown.</p>
 */
public final class BootstrapHandle implements AutoCloseable {
    private final Object participant;
    private final RapunzelContext context;
    private final BootstrapOwnerRole role;
    private final Runnable closeAction;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Creates a bootstrap handle.
     *
     * @param participant the plugin or module that owns or borrows this handle
     * @param context     the associated RapunzelContext
     * @param role        the participant's role
     * @param closeAction the action to run on close
     */
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

    /**
     * Returns the participant associated with this handle.
     *
     * @return the participant object
     */
    public @NotNull Object participant() {
        return participant;
    }

    /**
     * Returns the context associated with this handle.
     *
     * @return the context
     */
    public @NotNull RapunzelContext context() {
        return context;
    }

    /**
     * Returns the role of this handle (OWNER or BORROWER).
     *
     * @return the role
     */
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
