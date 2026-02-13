package de.t14d3.rapunzellib.commands.paper;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

final class PaperLifecycleCommandRegistrationSupport {
    private PaperLifecycleCommandRegistrationSupport() {
    }

    static <O extends LifecycleEventOwner> void register(
        @NotNull LifecycleEventManager<O> lifecycleManager,
        @NotNull Consumer<ReloadableRegistrarEvent<Commands>> registration
    ) {
        register(lifecycleManager, LifecycleEvents.COMMANDS, registration);
    }

    static <O extends LifecycleEventOwner> void register(
        @NotNull LifecycleEventManager<O> lifecycleManager,
        @NotNull LifecycleEventType<? super O, ? extends ReloadableRegistrarEvent<Commands>, ?> eventType,
        @NotNull Consumer<ReloadableRegistrarEvent<Commands>> registration
    ) {
        Objects.requireNonNull(lifecycleManager, "lifecycleManager");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(registration, "registration");
        lifecycleManager.registerEventHandler(eventType, registration::accept);
    }
}
