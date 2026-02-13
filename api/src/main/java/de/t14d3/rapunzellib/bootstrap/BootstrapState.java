package de.t14d3.rapunzellib.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class BootstrapState {
    private final @Nullable RapunzelContext context;
    private final @Nullable Object ownerToken;
    private final int borrowerCount;
    private final @Nullable PlatformBootstrapHost registeredPlatformHost;

    public BootstrapState(
        @Nullable RapunzelContext context,
        @Nullable Object ownerToken,
        int borrowerCount,
        @Nullable PlatformBootstrapHost registeredPlatformHost
    ) {
        if (borrowerCount < 0) {
            throw new IllegalArgumentException("borrowerCount must be >= 0");
        }
        if ((context == null) != (ownerToken == null)) {
            throw new IllegalArgumentException("context and ownerToken must either both be present or both be absent");
        }
        this.context = context;
        this.ownerToken = ownerToken;
        this.borrowerCount = borrowerCount;
        this.registeredPlatformHost = registeredPlatformHost;
    }

    public boolean isBootstrapped() {
        return context != null;
    }

    public @NotNull Optional<RapunzelContext> context() {
        return Optional.ofNullable(context);
    }

    public @NotNull Optional<Object> ownerToken() {
        return Optional.ofNullable(ownerToken);
    }

    public int ownerCount() {
        return ownerToken == null ? 0 : 1;
    }

    public int borrowerCount() {
        return borrowerCount;
    }

    public int participantCount() {
        return ownerCount() + borrowerCount;
    }

    public @NotNull Optional<PlatformBootstrapHost> registeredPlatformHost() {
        return Optional.ofNullable(registeredPlatformHost);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BootstrapState that)) return false;
        return borrowerCount == that.borrowerCount
            && Objects.equals(context, that.context)
            && Objects.equals(ownerToken, that.ownerToken)
            && Objects.equals(registeredPlatformHost, that.registeredPlatformHost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, ownerToken, borrowerCount, registeredPlatformHost);
    }
}
