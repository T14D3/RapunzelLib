package de.t14d3.rapunzellib.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * An immutable snapshot of the RapunzelLib bootstrap state.
 */
public final class BootstrapState {
    private final @Nullable RapunzelContext context;
    private final @Nullable Object ownerToken;
    private final int borrowerCount;
    private final @Nullable PlatformBootstrapHost registeredPlatformHost;

    /**
     * Creates a bootstrap state snapshot.
     *
     * @param context                 the active context, or null
     * @param ownerToken              the owner token, or null
     * @param borrowerCount           the number of borrowers
     * @param registeredPlatformHost  the registered platform host, or null
     */
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

    /**
     * Checks whether a context has been bootstrapped.
     *
     * @return true if bootstrapped
     */
    public boolean isBootstrapped() {
        return context != null;
    }

    /**
     * Returns the active context, if any.
     *
     * @return an {@link Optional} containing the context, or empty if not bootstrapped
     */
    public @NotNull Optional<RapunzelContext> context() {
        return Optional.ofNullable(context);
    }

    /**
     * Returns the owner token, if any.
     *
     * @return an {@link Optional} containing the owner token, or empty if not bootstrapped
     */
    public @NotNull Optional<Object> ownerToken() {
        return Optional.ofNullable(ownerToken);
    }

    /**
     * Returns the number of owner participants.
     *
     * @return the owner count (0 or 1)
     */
    public int ownerCount() {
        return ownerToken == null ? 0 : 1;
    }

    /**
     * Returns the number of borrower participants.
     *
     * @return the borrower count
     */
    public int borrowerCount() {
        return borrowerCount;
    }

    /**
     * Returns the total number of participants (owners + borrowers).
     *
     * @return the participant count
     */
    public int participantCount() {
        return ownerCount() + borrowerCount;
    }

    /**
     * Returns the registered platform bootstrap host, if any.
     *
     * @return an {@link Optional} containing the host, or empty if not registered
     */
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
