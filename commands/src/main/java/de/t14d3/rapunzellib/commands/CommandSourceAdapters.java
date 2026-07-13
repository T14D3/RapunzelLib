package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Holds a collection of {@link CommandSourceAdapter} instances for a specific platform.
 * <p>
 * When a platform source object needs to be wrapped, each adapter is tested
 * sequentially via {@link CommandSourceAdapter#supports(Object)} until one matches,
 * then its {@link CommandSourceAdapter#wrap(Object)} is invoked.
 * </p>
 */
public final class CommandSourceAdapters {
    
    private final PlatformId platformId;
    
    private final List<CommandSourceAdapter> adapters;

    public CommandSourceAdapters(@NotNull PlatformId platformId, @NotNull List<CommandSourceAdapter> adapters) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.adapters = List.copyOf(Objects.requireNonNull(adapters, "adapters"));
    }

    public @NotNull PlatformId platformId() {
        return platformId;
    }

    public @NotNull List<CommandSourceAdapter> adapters() {
        return adapters;
    }

    /**
     * Wraps a platform source into an {@link RCommandSource} using the first matching adapter.
     *
     * @param source the platform source object
     * @return the wrapped command source
     * @throws IllegalArgumentException if no adapter supports the source
     */
    public @NotNull RCommandSource wrap(@NotNull Object source) {
        Objects.requireNonNull(source, "source");
        for (CommandSourceAdapter adapter : adapters) {
            if (adapter.supports(source)) {
                return adapter.wrap(source);
            }
        }
        throw new IllegalArgumentException(
            "No command source adapter for platform " + platformId + " and source type " + source.getClass().getName()
        );
    }
}
