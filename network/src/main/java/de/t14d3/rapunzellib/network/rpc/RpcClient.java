package de.t14d3.rapunzellib.network.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Simple RPC client for cross-server communication.
 * Underlying transport is determined by the {@link Messenger} instance.
 */
public final class RpcClient implements AutoCloseable {
    private final Messenger messenger;
    private final Scheduler scheduler;
    private final Logger logger;
    private final Duration defaultTimeout;
    private final Gson gson;
    private final NetworkEventBus bus;
    private final NetworkEventBus.Subscription responseSubscription;

    private final Map<String, PendingRequest<?>> pending = new ConcurrentHashMap<>();
    private volatile boolean closed;

    /**
     * Creates a new RPC client with the given messenger, scheduler, and logger.
     *
     * @param messenger the messenger to use for RPC
     * @param scheduler the scheduler to use for RPC
     * @param logger the logger to use for RPC
     */
    public RpcClient(Messenger messenger, Scheduler scheduler, Logger logger) {
        this(messenger, scheduler, logger, Duration.ofSeconds(3), new Gson());
    }
    /**
     * Creates a new RPC client with the given messenger, scheduler, and logger.
     *
     * @param messenger the messenger to use for RPC
     * @param scheduler the scheduler to use for RPC
     * @param logger the logger to use for RPC
     * @param defaultTimeout the default timeout for RPC requests
     */
    public RpcClient(Messenger messenger, Scheduler scheduler, Logger logger, Duration defaultTimeout) {
        this(messenger, scheduler, logger, defaultTimeout, new Gson());
    }
    /**
     * Creates a new RPC client with the given messenger, scheduler, and logger.
     *
     * @param messenger the messenger to use for RPC
     * @param scheduler the scheduler to use for RPC
     * @param logger the logger to use for RPC
     * @param defaultTimeout the default timeout for RPC requests
     * @param gson the GSON instance to use for RPC requests
     */
    public RpcClient(Messenger messenger, Scheduler scheduler, Logger logger, Duration defaultTimeout, Gson gson) {
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.defaultTimeout = Objects.requireNonNull(defaultTimeout, "defaultTimeout");
        this.gson = Objects.requireNonNull(gson, "gson");

        this.bus = new NetworkEventBus(messenger, gson);
        this.responseSubscription = bus.register(
            RpcChannels.RESPONSE,
            RpcResponse.class,
                this::handleResponse
        );
    }

    public <T> CompletableFuture<T> callProxy(String service, String method, Object payload, Class<T> resultType) {
        return callProxy(service, method, payload, resultType, null);
    }

    public <T> CompletableFuture<T> callProxy(String service, String method, Object payload, Type resultType) {
        return callProxy(service, method, payload, resultType, null);
    }

    public <T> CompletableFuture<T> callProxy(String service, String method, Object payload, Type resultType, Duration timeout) {
        return call(Target.PROXY, null, service, method, payload, resultType, timeout);
    }

    public <T> CompletableFuture<T> callServer(String serverName, String service, String method, Object payload, Class<T> resultType) {
        return callServer(serverName, service, method, payload, resultType, null);
    }

    public <T> CompletableFuture<T> callServer(String serverName, String service, String method, Object payload, Type resultType) {
        return callServer(serverName, service, method, payload, resultType, null);
    }

    public <T> CompletableFuture<T> callServer(String serverName, String service, String method, Object payload, Type resultType, Duration timeout) {
        return call(Target.SERVER, serverName, service, method, payload, resultType, timeout);
    }

    private <T> CompletableFuture<T> call(
        Target target,
        String targetServerName,
        String service,
        String method,
        Object payload,
        Type resultType,
        Duration timeout
    ) {
        Objects.requireNonNull(target, "target");
        if (target == Target.SERVER && (targetServerName == null || targetServerName.isBlank())) {
            throw new IllegalArgumentException("targetServerName cannot be null/blank for SERVER");
        }
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("service cannot be null/blank");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method cannot be null/blank");
        }
        Objects.requireNonNull(resultType, "resultType");

        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("RpcClient is closed"));
        }

        if (!messenger.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                "Network messenger is not connected (" + messenger.getClass().getSimpleName() + ")"
            ));
        }

        Duration effectiveTimeout = effectiveTimeout(timeout);
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<T> future = new CompletableFuture<>();

        ScheduledTask timeoutTask = scheduler.runLater(effectiveTimeout, () -> {
            PendingRequest<?> removed = pending.remove(requestId);
            if (removed == null) return;
            removed.future.completeExceptionally(new TimeoutException(
                "RPC request timed out: " + removed.service + "#" + removed.method
            ));
        });

        pending.put(requestId, new PendingRequest<>(future, resultType, timeoutTask, service, method));
        future.whenComplete((result, error) -> {
            PendingRequest<?> removed = pending.remove(requestId);
            if (removed == null) return;
            try {
                removed.timeoutTask.cancel();
            } catch (Exception ignored) {
            }
        });

        RpcRequest request = new RpcRequest(
            requestId,
            service,
            method,
            gson.toJsonTree(payload),
            System.currentTimeMillis()
        );

        try {
            if (target == Target.PROXY) {
                bus.sendToProxy(RpcChannels.REQUEST, request);
            } else {
                bus.sendToServer(RpcChannels.REQUEST, targetServerName, request);
            }
        } catch (Exception e) {
            PendingRequest<?> removed = pending.remove(requestId);
            if (removed != null) {
                try {
                    removed.timeoutTask.cancel();
                } catch (Exception ignored) {
                }
            }
            future.completeExceptionally(e);
        }

        return future;
    }

    private Duration effectiveTimeout(Duration requested) {
        if (requested == null) return defaultTimeout;
        if (requested.isNegative()) return defaultTimeout;
        if (requested.isZero()) return defaultTimeout;
        return requested;
    }

    private void handleResponse(RpcResponse response, String sourceServer) {
        if (response == null || response.requestId() == null) return;

        PendingRequest<?> req = pending.remove(response.requestId());
        if (req == null) return;
        req.timeoutTask.cancel();

        if (!response.ok()) {
            String message = (response.error() == null || response.error().isBlank())
                ? "Remote returned an error"
                : response.error();
            req.future.completeExceptionally(new RpcException(
                response.requestId(),
                req.service,
                req.method,
                message,
                sourceServer
            ));
            return;
        }

        try {
            req.completeWithResult(gson, response.result());
        } catch (Exception e) {
            logger.warn("Failed to parse RPC response {}#{}: {}", req.service, req.method, e.getMessage());
            req.future.completeExceptionally(e);
        }
    }

    @Override
    public void close() {
        closed = true;
        responseSubscription.close();

        for (PendingRequest<?> req : pending.values()) {
            try {
                req.timeoutTask.cancel();
            } catch (Exception ignored) {
            }
            req.future.completeExceptionally(new IllegalStateException("RpcClient closed"));
        }
        pending.clear();
    }

    private enum Target {
        PROXY,
        SERVER
    }

    private static final class PendingRequest<T> {
        private final CompletableFuture<T> future;
        private final Type resultType;
        private final ScheduledTask timeoutTask;
        private final String service;
        private final String method;

        private PendingRequest(
            CompletableFuture<T> future,
            Type resultType,
            ScheduledTask timeoutTask,
            String service,
            String method
        ) {
            this.future = Objects.requireNonNull(future, "future");
            this.resultType = Objects.requireNonNull(resultType, "resultType");
            this.timeoutTask = Objects.requireNonNull(timeoutTask, "timeoutTask");
            this.service = Objects.requireNonNull(service, "service");
            this.method = Objects.requireNonNull(method, "method");
        }

        private void completeWithResult(Gson gson, JsonElement resultJsonElement) {
            @SuppressWarnings("unchecked")
            T parsed = gson.fromJson(resultJsonElement, resultType);
            if (parsed == null && resultType != Void.class) {
                future.completeExceptionally(new IllegalStateException("Remote returned no result"));
            } else {
                future.complete(parsed);
            }
        }
    }
}
