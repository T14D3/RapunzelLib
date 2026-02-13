package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

public interface SharedBrigadierCommandRegistrar<S> {
    @NotNull PlatformId platformId();

    @NotNull Class<S> sourceType();

    void registerSharedCommands(@NotNull CommandDispatcher<S> dispatcher);
}
