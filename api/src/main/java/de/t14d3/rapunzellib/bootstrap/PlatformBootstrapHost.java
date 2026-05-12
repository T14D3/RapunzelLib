package de.t14d3.rapunzellib.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A platform host that can participate in automatic RapunzelLib bootstrapping.
 */
public interface PlatformBootstrapHost {
    /**
     * Returns the token used to identify this host as a lifecycle owner.
     *
     * @return the owner token
     */
    @NotNull Object ownerToken();

    /**
     * Returns a human-readable display name for this host.
     *
     * @return the display name
     */
    @NotNull String displayName();

    /**
     * Attempts to create a context for the given bootstrap caller.
     *
     * @param bootstrapCaller the caller requesting the context
     * @param contextFactory  the factory to create the context
     * @return an {@link Optional} containing the context, or empty if not available
     */
    @NotNull Optional<? extends RapunzelContext> tryCreateContext(
        @NotNull Object bootstrapCaller,
        @NotNull Supplier<? extends RapunzelContext> contextFactory
    );
}
