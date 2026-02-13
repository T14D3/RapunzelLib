package de.t14d3.rapunzellib.bootstrap;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class BoundPlatformBootstrapHost<T> implements PlatformBootstrapHost {
    private final String defaultDisplayName;
    private final AtomicReference<T> boundOwner = new AtomicReference<>();

    protected BoundPlatformBootstrapHost(@NotNull String defaultDisplayName) {
        this.defaultDisplayName = Objects.requireNonNull(defaultDisplayName, "defaultDisplayName");
    }

    protected final void bindOwner(@NotNull T owner) {
        boundOwner.set(Objects.requireNonNull(owner, "owner"));
    }

    protected final void shutdownAndUnbind(@NotNull T owner) {
        T current = Objects.requireNonNull(owner, "owner");
        Rapunzel.shutdown(ownerToken());
        boundOwner.compareAndSet(current, null);
    }

    protected final void unbind(@NotNull T owner) {
        boundOwner.compareAndSet(Objects.requireNonNull(owner, "owner"), null);
    }

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

    protected abstract @NotNull String displayName(@NotNull T owner);
}
