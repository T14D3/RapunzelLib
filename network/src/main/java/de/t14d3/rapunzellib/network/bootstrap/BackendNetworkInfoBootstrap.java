package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.network.info.NetworkInfoRpc;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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

        registerLocalPlayersResponder(context, gateway);

        NetworkInfoClient networkInfo = new NetworkInfoClient(gateway, scheduler, logger);
        context.registerLinked(NetworkInfoClient.class, networkInfo, NetworkInfoService.class);
        return networkInfo;
    }

    /**
     * Registers the backend-side {@link NetworkInfoRpc#LIST_LOCAL_PLAYERS_METHOD}
     * responder so the proxy can merge backend-local players into its
     * {@code list_players} answer. This makes players that connect directly to
     * a backend (without traversing the proxy) resolvable network-wide.
     */
    private static void registerLocalPlayersResponder(
        @NotNull RapunzelContext context,
        @NotNull NetworkRuntimeGateway gateway
    ) {
        gateway.register(NetworkInfoRpc.LIST_LOCAL_PLAYERS_METHOD, (_ignored, sourceServer) ->
            CompletableFuture.completedFuture(context.players().online().stream()
                .map(player -> {
                    String name = player.name();
                    if (name == null || name.isBlank()) return null;
                    return new NetworkPlayerInfo(player.uuid(), name, effectiveLocalName(gateway));
                })
                .filter(Objects::nonNull)
                .toList())
        );
    }

    /**
     * Resolves the local server name at response time.
     *
     * <p>The runtime's {@code localName()} is fixed during transport bootstrap,
     * when the plugin messenger may still report {@code "unknown"} (the
     * consumer binds the configured server name afterwards). Reporting that
     * stale snapshot as the player's server name poisons the network player
     * registry: cross-server targeting then resolves players to
     * {@code "unknown"} and the proxy drops envelopes addressed to that name.
     * Prefer the canonical messenger's live name (which reflects the bound
     * name); fall back to the runtime name.</p>
     */
    private static @NotNull String effectiveLocalName(@NotNull NetworkRuntimeGateway gateway) {
        try {
            Messenger messenger = gateway.runtime().canonicalMessenger();
            String name = messenger != null ? messenger.getServerName() : null;
            if (name != null && !name.isBlank() && !"unknown".equalsIgnoreCase(name.trim())) {
                return name.trim();
            }
        } catch (Exception ignored) {
            // Fall through to the runtime name.
        }
        String runtimeName = gateway.runtime().localName();
        return (runtimeName == null || runtimeName.isBlank()) ? "unknown" : runtimeName;
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
