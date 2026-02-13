package de.t14d3.rapunzellib.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public interface PlatformBootstrapHost {
    @NotNull Object ownerToken();

    @NotNull String displayName();

    @NotNull Optional<? extends RapunzelContext> tryCreateContext(
        @NotNull Object bootstrapCaller,
        @NotNull Supplier<? extends RapunzelContext> contextFactory
    );
}
