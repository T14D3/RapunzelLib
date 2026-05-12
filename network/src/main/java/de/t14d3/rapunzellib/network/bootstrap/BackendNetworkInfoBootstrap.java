package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Bootstraps network info services for backend servers.
 */
public final class BackendNetworkInfoBootstrap {
    private BackendNetworkInfoBootstrap() {
    }

    public static @NotNull NetworkInfoClient registerClient(
        @NotNull RapunzelContext context,
        @NotNull Messenger messenger,
        @NotNull Scheduler scheduler,
        @NotNull Logger logger
    ) {
        return registerClient(context, DefaultNetworkRuntimeGateway.compatibility(messenger), scheduler, logger);
    }

    public static @NotNull NetworkInfoClient registerClient(
        @NotNull RapunzelContext context,
        @NotNull NetworkRuntimeGateway gateway,
        @NotNull Scheduler scheduler,
        @NotNull Logger logger
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(gateway, "gateway");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(logger, "logger");

        NetworkInfoClient networkInfo = new NetworkInfoClient(gateway, scheduler, logger);
        context.registerLinked(NetworkInfoClient.class, networkInfo, NetworkInfoService.class);
        return networkInfo;
    }

    public static <M extends Messenger> @NotNull NetworkInfoClient registerClientAndBindServerName(
        @NotNull RapunzelContext context,
        @NotNull NetworkRuntimeGateway gateway,
        @NotNull Scheduler scheduler,
        @NotNull Logger logger,
        @NotNull M messenger,
        @NotNull BiConsumer<? super M, String> serverNameBinder
    ) {
        Objects.requireNonNull(messenger, "messenger");
        Objects.requireNonNull(serverNameBinder, "serverNameBinder");

        NetworkInfoClient networkInfo = registerClient(context, gateway, scheduler, logger);
        bindServerName(context, scheduler, logger, networkInfo, messenger, serverNameBinder);
        return networkInfo;
    }

    public static <M extends Messenger> void bindServerName(
        @NotNull RapunzelContext context,
        @NotNull Scheduler scheduler,
        @NotNull Logger logger,
        @NotNull NetworkInfoClient networkInfo,
        @NotNull M messenger,
        @NotNull BiConsumer<? super M, String> serverNameBinder
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(networkInfo, "networkInfo");
        Objects.requireNonNull(messenger, "messenger");
        Objects.requireNonNull(serverNameBinder, "serverNameBinder");

        Runnable resolve = () -> networkInfo.networkServerName()
            .thenAccept(serverName -> {
                if (serverName != null && !serverName.isBlank()) {
                    serverNameBinder.accept(messenger, serverName);
                }
            })
            .exceptionally(error -> {
                logger.debug("Failed to resolve backend network server name", error);
                return null;
            });

        resolve.run();
        registerRepeatingTask(context, scheduler, Duration.ofSeconds(1), Duration.ofSeconds(5), () -> {
            if (hasResolvedServerName(messenger)) {
                return false;
            }
            if (!messenger.isConnected()) {
                return true;
            }
            resolve.run();
            return true;
        });
    }

    private static boolean hasResolvedServerName(@NotNull Messenger messenger) {
        String serverName = messenger.getServerName();
        return serverName != null
            && !serverName.isBlank()
            && !"unknown".equalsIgnoreCase(serverName.trim());
    }

    public static void registerRepeatingTask(
        @NotNull RapunzelContext context,
        @NotNull Scheduler scheduler,
        @NotNull Duration initialDelay,
        @NotNull Duration interval,
        @NotNull BooleanSupplier taskBody
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(initialDelay, "initialDelay");
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(taskBody, "taskBody");

        AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
        taskRef.set(scheduler.runRepeating(initialDelay, interval, () -> {
            if (!taskBody.getAsBoolean()) {
                ScheduledTask task = taskRef.getAndSet(null);
                if (task != null) {
                    task.cancel();
                }
            }
        }));
        context.registerCloseable(() -> {
            ScheduledTask task = taskRef.getAndSet(null);
            if (task != null) {
                task.cancel();
            }
        });
    }
}
