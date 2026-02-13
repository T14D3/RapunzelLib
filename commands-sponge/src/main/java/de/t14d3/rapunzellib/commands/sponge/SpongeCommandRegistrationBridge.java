package de.t14d3.rapunzellib.commands.sponge;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;

import java.util.Objects;

final class SpongeCommandRegistrationBridge implements AutoCloseable {
    private final PluginContainer plugin;
    private final SpongeCommandRegistrationSupport support;

    SpongeCommandRegistrationBridge(
        @NotNull PluginContainer plugin,
        @NotNull SpongeCommandRegistrationSupport support
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.support = Objects.requireNonNull(support, "support");
    }

    void register() {
        Sponge.eventManager().registerListeners(plugin, this);
    }

    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Command.Raw> event) {
        support.registerSharedCommands(event, plugin);
    }

    @Override
    public void close() {
        Sponge.eventManager().unregisterListeners(this);
    }
}
