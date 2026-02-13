package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

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
