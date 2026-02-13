package de.t14d3.rapunzellib.network.runtime;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface NetworkRuntimeGateway extends AutoCloseable {
    @NotNull NetworkRuntime runtime();

    boolean isConnected();

    <T> void publish(@NotNull NetworkTopic<T> topic, @NotNull NetworkPath path, T payload);

    <T> @NotNull Subscription subscribe(@NotNull NetworkTopic<T> topic, @NotNull TopicListener<T> listener);

    default <T> void publishToAll(@NotNull NetworkTopic<T> topic, T payload) {
        publish(topic, NetworkPath.all(), payload);
    }

    default <T> void publishToProxy(@NotNull NetworkTopic<T> topic, T payload) {
        publish(topic, NetworkPath.proxy(), payload);
    }

    default <T> void publishToServer(@NotNull NetworkTopic<T> topic, @NotNull String serverName, T payload) {
        publish(topic, NetworkPath.server(serverName), payload);
    }

    default <Req, Res> @NotNull CompletableFuture<Res> callProxy(@NotNull RpcMethod<Req, Res> method, Req payload) {
        return call(NetworkPath.proxy(), method, payload, null);
    }

    default <Req, Res> @NotNull CompletableFuture<Res> callProxy(
        @NotNull RpcMethod<Req, Res> method,
        Req payload,
        Duration timeout
    ) {
        return call(NetworkPath.proxy(), method, payload, timeout);
    }

    default <Req, Res> @NotNull CompletableFuture<Res> callServer(
        @NotNull String serverName,
        @NotNull RpcMethod<Req, Res> method,
        Req payload
    ) {
        return call(NetworkPath.server(serverName), method, payload, null);
    }

    default <Req, Res> @NotNull CompletableFuture<Res> callServer(
        @NotNull String serverName,
        @NotNull RpcMethod<Req, Res> method,
        Req payload,
        Duration timeout
    ) {
        return call(NetworkPath.server(serverName), method, payload, timeout);
    }

    default <Req, Res> @NotNull CompletableFuture<Res> call(
        @NotNull NetworkPath path,
        @NotNull RpcMethod<Req, Res> method,
        Req payload
    ) {
        return call(path, method, payload, null);
    }

    <Req, Res> @NotNull CompletableFuture<Res> call(
        @NotNull NetworkPath path,
        @NotNull RpcMethod<Req, Res> method,
        Req payload,
        Duration timeout
    );

    <Req, Res> @NotNull Subscription register(
        @NotNull RpcMethod<Req, Res> method,
        @NotNull RpcHandler<Req, Res> handler
    );

    @Override
    void close();

    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    @FunctionalInterface
    interface TopicListener<T> {
        void onMessage(T payload, String sourceServer);
    }

    @FunctionalInterface
    interface RpcHandler<Req, Res> {
        @NotNull CompletableFuture<Res> handle(Req request, String sourceServer);

        static <Req, Res> @NotNull RpcHandler<Req, Res> sync(@NotNull SyncRpcHandler<Req, Res> handler) {
            Objects.requireNonNull(handler, "handler");
            return (request, sourceServer) -> CompletableFuture.completedFuture(handler.handle(request, sourceServer));
        }
    }

    @FunctionalInterface
    interface SyncRpcHandler<Req, Res> {
        Res handle(Req request, String sourceServer);
    }
}
