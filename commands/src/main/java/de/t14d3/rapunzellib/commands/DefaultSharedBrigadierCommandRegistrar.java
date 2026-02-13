package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class DefaultSharedBrigadierCommandRegistrar<S> implements SharedBrigadierCommandRegistrar<S> {
    private final PlatformId platformId;
    private final Class<S> sourceType;
    private final RCommandService commandService;
    private final CommandSourceAdapters adapters;

    DefaultSharedBrigadierCommandRegistrar(
        @NotNull PlatformId platformId,
        @NotNull Class<S> sourceType,
        @NotNull RCommandService commandService,
        @NotNull CommandSourceAdapters adapters
    ) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.commandService = Objects.requireNonNull(commandService, "commandService");
        this.adapters = Objects.requireNonNull(adapters, "adapters");
    }

    @Override
    public @NotNull PlatformId platformId() {
        return platformId;
    }

    @Override
    public @NotNull Class<S> sourceType() {
        return sourceType;
    }

    @Override
    public void registerSharedCommands(@NotNull CommandDispatcher<S> dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        commandService.sharedTree().attachMapped(dispatcher, source -> adapters.wrap(sourceType.cast(source)));
    }
}
