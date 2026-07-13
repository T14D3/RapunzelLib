package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Supports runtime command registration by synchronizing shared commands
 * with the Brigadier dispatcher and scheduling data-pack reloads when
 * the command service changes.
 * <p>
 * Implements {@link AutoCloseable} to clean up the service subscription.
 */
public final class SharedRuntimeCommandRegistrationSupport implements AutoCloseable {
    private final RCommandService commandService;
    private final Scheduler scheduler;
    private final Runnable reloadAction;
    private final RCommandService.Subscription subscription;
    private final Object stateLock = new Object();

    private boolean dispatcherObserved;
    private boolean reloadScheduled;
    private boolean dirty;
    private Set<String> registeredLabels = Set.of();

    public SharedRuntimeCommandRegistrationSupport(
        @NotNull RCommandService commandService,
        @NotNull Scheduler scheduler,
        @NotNull MinecraftServer server
    ) {
        this(commandService, scheduler, () -> SharedCommandDataReloader.reload(server));
    }

    /**
     * Internal constructor with a custom reload action.
     *
     * @param commandService the command service
     * @param scheduler      the scheduler
     * @param reloadAction   the reload action
     */
    SharedRuntimeCommandRegistrationSupport(
        @NotNull RCommandService commandService,
        @NotNull Scheduler scheduler,
        @NotNull Runnable reloadAction
    ) {
        this.commandService = Objects.requireNonNull(commandService, "commandService");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.reloadAction = Objects.requireNonNull(reloadAction, "reloadAction");
        this.subscription = commandService.subscribe(this::onCommandServiceChange);
    }

    /**
     * Synchronizes the dispatcher by removing previous command labels,
     * executing the registration, and updating the tracked label set.
     *
     * @param dispatcher   the command dispatcher
     * @param registration the registration action
     * @param <S>          the command source type
     */
    public <S> void sync(@NotNull CommandDispatcher<S> dispatcher, @NotNull Runnable registration) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        Objects.requireNonNull(registration, "registration");

        Set<String> previousLabels;
        synchronized (stateLock) {
            dispatcherObserved = true;
            previousLabels = registeredLabels;
        }

        BrigadierCommandNodeAccess.removeCommands(dispatcher.getRoot(), previousLabels);
        registration.run();

        synchronized (stateLock) {
            registeredLabels = currentLabels();
            dirty = false;
        }
    }

    @Override
    public void close() {
        subscription.close();
    }

    private void onCommandServiceChange(@NotNull RCommandServiceChange change) {
        synchronized (stateLock) {
            dirty = true;
        }

        if (change.queued()) {
            return;
        }

        if (change.type() != RCommandServiceChange.Type.FLUSH_REQUESTED
            && change.type() != RCommandServiceChange.Type.REGISTERED
            && change.type() != RCommandServiceChange.Type.UNREGISTERED) {
            return;
        }

        scheduleReload();
    }

    private void scheduleReload() {
        synchronized (stateLock) {
            if (!dispatcherObserved || reloadScheduled || !dirty) {
                return;
            }
            reloadScheduled = true;
        }

        scheduler.runLater(Duration.ZERO, () -> {
            try {
                reloadAction.run();
            } finally {
                synchronized (stateLock) {
                    reloadScheduled = false;
                }
            }
        });
    }

    private @NotNull Set<String> currentLabels() {
        Set<String> labels = new LinkedHashSet<>();
        for (RCommandNode<RCommandSource> root : commandService.roots()) {
            labels.add(root.getName());
            labels.addAll(root.getAliases());
        }
        return Set.copyOf(labels);
    }
}
