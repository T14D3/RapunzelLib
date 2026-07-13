package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Default implementation of {@link SharedBrigadierCommandRegistrar}.
 * <p>
 * Attaches all registered Rapunzel commands from an {@link RCommandService}
 * to a Brigadier {@link CommandDispatcher}, wrapping native sources via
 * the provided {@link CommandSourceAdapters}.
 * </p>
 *
 * @param <S> the Brigadier command source type
 */
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

    /**
     * Registers all shared Rapunzel commands into the given Brigadier dispatcher.
     *
     * @param dispatcher the Brigadier command dispatcher
     */
    @Override
    public void registerSharedCommands(@NotNull CommandDispatcher<S> dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        commandService.sharedTree().attachMapped(dispatcher, source -> adapters.wrap(sourceType.cast(source)));
    }
}
