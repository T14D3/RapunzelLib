package de.t14d3.rapunzellib.bootstrap;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Abstract base for platform bootstrap hosts that bind to a typed owner.
 *
 * <p>Manages lifecycle bind/unbind of a platform owner object and delegates
 * context creation to subclasses.</p>
 *
 * @param <T> the owner type bound to this host
 */
public abstract class BoundPlatformBootstrapHost<T> implements PlatformBootstrapHost {
    private final String defaultDisplayName;
    private final AtomicReference<T> boundOwner = new AtomicReference<>();

    /**
     * Creates a new bound platform bootstrap host.
     *
     * @param defaultDisplayName fallback display name when no owner is bound
     */
    protected BoundPlatformBootstrapHost(@NotNull String defaultDisplayName) {
        this.defaultDisplayName = Objects.requireNonNull(defaultDisplayName, "defaultDisplayName");
    }

    /**
     * Binds this host to the given owner.
     *
     * @param owner the owner to bind, not null
     */
    protected final void bindOwner(@NotNull T owner) {
        boundOwner.set(Objects.requireNonNull(owner, "owner"));
    }

    /**
     * Shuts down Rapunzel for the current owner and unbinds.
     *
     * @param owner the currently bound owner to shut down
     */
    protected final void shutdownAndUnbind(@NotNull T owner) {
        T current = Objects.requireNonNull(owner, "owner");
        Rapunzel.shutdown(ownerToken());
        boundOwner.compareAndSet(current, null);
    }

    /**
     * Unbinds the given owner without shutting down.
     *
     * @param owner the owner to unbind
     */
    protected final void unbind(@NotNull T owner) {
        boundOwner.compareAndSet(Objects.requireNonNull(owner, "owner"), null);
    }

    /**
     * Returns the currently bound owner, if any.
     *
     * @return optional containing the bound owner
     */
    protected final @NotNull Optional<T> boundOwner() {
        return Optional.ofNullable(boundOwner.get());
    }

    @Override
    public final @NotNull Object ownerToken() {
        return this;
    }

    @Override
    public final @NotNull String displayName() {
        return boundOwner().map(this::displayName).orElse(defaultDisplayName);
    }

    @Override
    public final @NotNull Optional<? extends RapunzelContext> tryCreateContext(
        @NotNull Object bootstrapCaller,
        @NotNull Supplier<? extends RapunzelContext> contextFactory
    ) {
        Objects.requireNonNull(bootstrapCaller, "bootstrapCaller");
        Objects.requireNonNull(contextFactory, "contextFactory");
        if (boundOwner.get() == null) {
            return Optional.empty();
        }
        return Optional.of(Objects.requireNonNull(contextFactory.get(), "contextFactory.get()"));
    }

    /**
     * Returns a display name for the given bound owner.
     *
     * @param owner the bound owner
     * @return display name string
     */
    protected abstract @NotNull String displayName(@NotNull T owner);
}
