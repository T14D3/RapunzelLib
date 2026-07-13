package de.t14d3.rapunzellib.network.runtime;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import de.t14d3.rapunzellib.network.rpc.RpcChannels;
import de.t14d3.rapunzellib.network.rpc.RpcClient;
import de.t14d3.rapunzellib.network.rpc.RpcRequest;
import de.t14d3.rapunzellib.network.rpc.RpcResponse;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultNetworkRuntimeGateway implements NetworkRuntimeGateway {
    private final NetworkRuntime runtime;
    private final NetworkEventBus bus;
    private final Gson gson;
    private final Logger logger;
    private final RpcClient rpcClient;
    private final Set<ManagedSubscription> subscriptions = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    /**
     * Creates a gateway with a runtime and default Gson.
     *
     * @param runtime the network runtime
     */
    public DefaultNetworkRuntimeGateway(@NotNull NetworkRuntime runtime) {
        this(runtime, null, LoggerFactory.getLogger(DefaultNetworkRuntimeGateway.class), JsonCodecs.gson());
    }

    /**
     * Creates a gateway with a runtime and custom Gson.
     *
     * @param runtime the network runtime
     * @param gson    the Gson instance
     */
    public DefaultNetworkRuntimeGateway(@NotNull NetworkRuntime runtime, @NotNull Gson gson) {
        this(runtime, null, LoggerFactory.getLogger(DefaultNetworkRuntimeGateway.class), gson);
    }

    /**
     * Creates a gateway with a runtime, scheduler, and logger.
     *
     * @param runtime   the network runtime
     * @param scheduler the scheduler
     * @param logger    the logger
     */
    public DefaultNetworkRuntimeGateway(@NotNull NetworkRuntime runtime, @NotNull Scheduler scheduler, @NotNull Logger logger) {
        this(runtime, Objects.requireNonNull(scheduler, "scheduler"), logger, JsonCodecs.gson());
    }

    /**
     * Creates a fully configured gateway.
     *
     * @param runtime   the network runtime
     * @param scheduler the scheduler (may be null, disables RPC)
     * @param logger    the logger
     * @param gson      the Gson instance
     */
    public DefaultNetworkRuntimeGateway(
        @NotNull NetworkRuntime runtime,
        Scheduler scheduler,
        @NotNull Logger logger,
        @NotNull Gson gson
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.gson = Objects.requireNonNull(gson, "gson");
        this.bus = new NetworkEventBus(runtime.canonicalMessenger(), gson);
        this.rpcClient = scheduler != null ? new RpcClient(this, scheduler, logger, java.time.Duration.ofSeconds(3), gson) : null;
    }

    /**
     * Creates a compatibility gateway from a messenger (infers runtime metadata).
     *
     * @param messenger the messenger
     * @return a compatibility gateway
     */
    public static @NotNull DefaultNetworkRuntimeGateway compatibility(@NotNull Messenger messenger) {
        return compatibility(messenger, JsonCodecs.gson());
    }

    /**
     * Creates a compatibility gateway from a messenger with custom Gson.
     *
     * @param messenger the messenger
     * @param gson      the Gson instance
     * @return a compatibility gateway
     */
    public static @NotNull DefaultNetworkRuntimeGateway compatibility(@NotNull Messenger messenger, @NotNull Gson gson) {
        Objects.requireNonNull(messenger, "messenger");
        Objects.requireNonNull(gson, "gson");
        String serverName = messenger.getServerName();
        String proxyName = messenger.getProxyServerName();
        NetworkNodeRole role = serverName.equalsIgnoreCase(proxyName) ? NetworkNodeRole.PROXY : NetworkNodeRole.BACKEND;
        NetworkRuntime runtime = new DefaultNetworkRuntime(
            role,
            serverName,
            proxyName,
            new NetworkLink(NetworkLinkKind.IN_MEMORY, messenger),
            Optional.empty(),
            messenger
        );
        return new DefaultNetworkRuntimeGateway(runtime, gson);
    }

    @Override
    public @NotNull NetworkRuntime runtime() {
        return runtime;
    }

    @Override
    public boolean isConnected() {
        return runtime.canonicalMessenger().isConnected();
    }

    @Override
    public <T> void publish(@NotNull NetworkTopic<T> topic, @NotNull NetworkPath path, T payload) {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(path, "path");
        ensureOpen();

        switch (path.target()) {
            case ALL -> bus.sendToAll(topic.channel(), payload);
            case PROXY -> bus.sendToProxy(topic.channel(), payload);
            case SERVER -> bus.sendToServer(topic.channel(), path.serverName(), payload);
        }
    }

    @Override
    public <T> @NotNull Subscription subscribe(@NotNull NetworkTopic<T> topic, @NotNull TopicListener<T> listener) {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(listener, "listener");
        ensureOpen();

        NetworkEventBus.Subscription delegate = bus.register(topic.channel(), topic.payloadType(), listener::onMessage);
        ManagedSubscription subscription = new ManagedSubscription(delegate);
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public <Req, Res> @NotNull CompletableFuture<Res> call(
        @NotNull NetworkPath path,
        @NotNull RpcMethod<Req, Res> method,
        Req payload,
        java.time.Duration timeout
    ) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(method, "method");
        ensureOpen();
        if (path.target() == NetworkPath.Target.ALL) {
            throw new IllegalArgumentException("RPC calls require a proxy or server path");
        }
        if (rpcClient == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("RPC support is not configured for this gateway"));
        }
        return switch (path.target()) {
            case PROXY -> rpcClient.callProxy(method, payload, timeout);
            case SERVER -> rpcClient.callServer(path.serverName(), method, payload, timeout);
            case ALL -> throw new IllegalArgumentException("RPC calls require a proxy or server path");
        };
    }

    @Override
    public <Req, Res> @NotNull Subscription register(
        @NotNull RpcMethod<Req, Res> method,
        @NotNull RpcHandler<Req, Res> handler
    ) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(handler, "handler");
        ensureOpen();

        return subscribe(RpcChannels.REQUEST_TOPIC, (request, sourceServer) -> {
            if (request == null || request.requestId() == null) {
                return;
            }
            if (!method.service().equals(request.service()) || !method.method().equals(request.method())) {
                return;
            }
            if (sourceServer == null || sourceServer.isBlank()) {
                return;
            }

            Req decoded;
            try {
                @SuppressWarnings("unchecked")
                Req parsed = (Req) gson.fromJson(request.payload(), method.requestType());
                decoded = parsed;
            } catch (Exception e) {
                sendRpcError(request.requestId(), sourceServer, e.getMessage());
                return;
            }

            CompletableFuture<Res> responseFuture;
            try {
                responseFuture = Objects.requireNonNull(handler.handle(decoded, sourceServer), "handler returned null future");
            } catch (Exception e) {
                sendRpcError(request.requestId(), sourceServer, e.getMessage());
                return;
            }

            responseFuture.whenComplete((result, error) -> {
                if (error != null) {
                    sendRpcError(request.requestId(), sourceServer, error.getMessage());
                    return;
                }
                publishToServer(
                    RpcChannels.RESPONSE_TOPIC,
                    sourceServer,
                    new RpcResponse(request.requestId(), true, gson.toJsonTree(result), null, System.currentTimeMillis())
                );
            });
        });
    }

    @Override
    public void close() {
        closed = true;
        for (ManagedSubscription subscription : Set.copyOf(subscriptions)) {
            subscription.close();
        }
        if (rpcClient != null) {
            rpcClient.close();
        }
    }

    private void sendRpcError(String requestId, String sourceServer, String message) {
        try {
            publishToServer(
                RpcChannels.RESPONSE_TOPIC,
                sourceServer,
                new RpcResponse(
                    requestId,
                    false,
                    null,
                    (message == null || message.isBlank()) ? "Remote returned an error" : message,
                    System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            logger.debug("Failed to send RPC error response for request {}", requestId, e);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("NetworkRuntimeGateway is closed");
        }
    }

    private final class ManagedSubscription implements Subscription {
        private final NetworkEventBus.Subscription delegate;
        private volatile boolean closed;

        private ManagedSubscription(NetworkEventBus.Subscription delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            subscriptions.remove(this);
            delegate.close();
        }
    }
}
