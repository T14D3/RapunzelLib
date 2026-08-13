package de.t14d3.rapunzellib.network.remote.handler;

import de.t14d3.rapunzellib.network.remote.rpc.ProxyServiceMethods;
import de.t14d3.rapunzellib.network.remote.rpc.Requests;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.testutil.TestNetworkSupport.TestNetwork;
import de.t14d3.rapunzellib.network.testutil.TestNetworkSupport.TestScheduler;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProxyRpcHandlerRegistrarTest {

    @Test
    void successfulConnectStoresLocationAndPollConsumesItExactlyOnce() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        UUID playerId = UUID.randomUUID();
        AtomicReference<String> connectedTarget = new AtomicReference<>();

        try (
            DefaultNetworkRuntimeGateway proxy = network.createGateway("velocity", "velocity", scheduler);
            DefaultNetworkRuntimeGateway backend = network.createGateway("lobby", "velocity", scheduler);
            NetworkRuntimeGateway.Subscription proxyHandlers = ProxyRpcHandlerRegistrar.register(
                proxy,
                (uuid, target) -> {
                    connectedTarget.set(target);
                    return CompletableFuture.completedFuture(true);
                }
            )
        ) {
            RWorldRef world = RWorldRef.of("lobby", RKey.of("minecraft", "world"));
            RLocation destination = RLocation.of(world, 10.5, 64.0, -20.25, 90f, 12f);

            Requests.BooleanResult connectResult = backend.callProxy(
                ProxyServiceMethods.PROXY_CONNECT_PLAYER,
                new Requests.ConnectToServerRequest(
                    playerId, "lobby", world, 10.5, 64.0, -20.25, 90f, 12f, true)
            ).join();

            assertTrue(connectResult.success());
            assertEquals("lobby", connectedTarget.get());

            Requests.PollDeferredTeleportResult first = backend.callProxy(
                ProxyServiceMethods.PROXY_POLL_DEFERRED_TELEPORT,
                new Requests.PollDeferredTeleportRequest(playerId)
            ).join();
            assertEquals(destination, first.location());

            // The poll consumes the entry: a second poll returns nothing.
            Requests.PollDeferredTeleportResult second = backend.callProxy(
                ProxyServiceMethods.PROXY_POLL_DEFERRED_TELEPORT,
                new Requests.PollDeferredTeleportRequest(playerId)
            ).join();
            assertNull(second.location());
        }
    }

    @Test
    void failedConnectDoesNotStoreLocation() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        UUID playerId = UUID.randomUUID();

        try (
            DefaultNetworkRuntimeGateway proxy = network.createGateway("velocity", "velocity", scheduler);
            DefaultNetworkRuntimeGateway backend = network.createGateway("lobby", "velocity", scheduler);
            NetworkRuntimeGateway.Subscription proxyHandlers = ProxyRpcHandlerRegistrar.register(
                proxy,
                (uuid, target) -> CompletableFuture.completedFuture(false)
            )
        ) {
            Requests.BooleanResult connectResult = backend.callProxy(
                ProxyServiceMethods.PROXY_CONNECT_PLAYER,
                new Requests.ConnectToServerRequest(
                    playerId, "lobby",
                    RWorldRef.of("lobby", RKey.of("minecraft", "world")),
                    10.5, 64.0, -20.25, 90f, 12f, true)
            ).join();

            assertFalse(connectResult.success());

            Requests.PollDeferredTeleportResult polled = backend.callProxy(
                ProxyServiceMethods.PROXY_POLL_DEFERRED_TELEPORT,
                new Requests.PollDeferredTeleportRequest(playerId)
            ).join();
            assertNull(polled.location());
        }
    }

    @Test
    void connectWithoutLocationDoesNotStoreAnything() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        UUID playerId = UUID.randomUUID();

        try (
            DefaultNetworkRuntimeGateway proxy = network.createGateway("velocity", "velocity", scheduler);
            DefaultNetworkRuntimeGateway backend = network.createGateway("lobby", "velocity", scheduler);
            NetworkRuntimeGateway.Subscription proxyHandlers = ProxyRpcHandlerRegistrar.register(
                proxy,
                (uuid, target) -> CompletableFuture.completedFuture(true)
            )
        ) {
            Requests.BooleanResult connectResult = backend.callProxy(
                ProxyServiceMethods.PROXY_CONNECT_PLAYER,
                new Requests.ConnectToServerRequest(
                    playerId, "lobby", null, 0, 0, 0, 0, 0, false)
            ).join();

            assertTrue(connectResult.success());

            Requests.PollDeferredTeleportResult polled = backend.callProxy(
                ProxyServiceMethods.PROXY_POLL_DEFERRED_TELEPORT,
                new Requests.PollDeferredTeleportRequest(playerId)
            ).join();
            assertNull(polled.location());
        }
    }

    @Test
    void nullConnectRequestIsRejectedWithoutCallingConnector() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        AtomicReference<String> connectorCall = new AtomicReference<>();

        try (
            DefaultNetworkRuntimeGateway proxy = network.createGateway("velocity", "velocity", scheduler);
            DefaultNetworkRuntimeGateway backend = network.createGateway("lobby", "velocity", scheduler);
            NetworkRuntimeGateway.Subscription proxyHandlers = ProxyRpcHandlerRegistrar.register(
                proxy,
                (uuid, target) -> {
                    connectorCall.set(target);
                    return CompletableFuture.completedFuture(true);
                }
            )
        ) {
            Requests.BooleanResult result = backend.callProxy(
                ProxyServiceMethods.PROXY_CONNECT_PLAYER,
                (Requests.ConnectToServerRequest) null
            ).join();

            assertFalse(result.success());
            assertNull(connectorCall.get());
        }
    }

    @Test
    void closingSubscriptionUnregistersHandlers() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        UUID playerId = UUID.randomUUID();

        try (
            DefaultNetworkRuntimeGateway proxy = network.createGateway("velocity", "velocity", scheduler);
            DefaultNetworkRuntimeGateway backend = network.createGateway("lobby", "velocity", scheduler)
        ) {
            NetworkRuntimeGateway.Subscription proxyHandlers = ProxyRpcHandlerRegistrar.register(
                proxy,
                (uuid, target) -> CompletableFuture.completedFuture(true)
            );
            proxyHandlers.close();

            // The poll handler is gone: the request is never answered, so the
            // call fails on the timeout path rather than returning a location.
            CompletableFuture<Requests.PollDeferredTeleportResult> pending = backend.callProxy(
                ProxyServiceMethods.PROXY_POLL_DEFERRED_TELEPORT,
                new Requests.PollDeferredTeleportRequest(playerId)
            );
            scheduler.triggerAll();
            assertThrows(Exception.class, pending::join);
        }
    }
}
