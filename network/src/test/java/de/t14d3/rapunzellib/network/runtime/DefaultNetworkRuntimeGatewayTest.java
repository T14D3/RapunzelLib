package de.t14d3.rapunzellib.network.runtime;

import de.t14d3.rapunzellib.network.testutil.TestNetworkSupport.TestNetwork;
import de.t14d3.rapunzellib.network.testutil.TestNetworkSupport.TestScheduler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DefaultNetworkRuntimeGatewayTest {
    @Test
    void publishesTopicsAndServesRpcCalls() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        NetworkTopic<TestPayload> topic = NetworkTopic.of("test:topic", TestPayload.class);
        RpcMethod<String, String> method = RpcMethod.of("test", "ping", String.class, String.class);
        AtomicReference<TestPayload> received = new AtomicReference<>();

        try (
            DefaultNetworkRuntimeGateway server = network.createGateway("server", "proxy", scheduler);
            DefaultNetworkRuntimeGateway client = network.createGateway("client", "proxy", scheduler);
            NetworkRuntimeGateway.Subscription topicSubscription = server.subscribe(topic, (payload, sourceServer) -> received.set(payload));
            NetworkRuntimeGateway.Subscription rpcSubscription = server.register(
                method,
                NetworkRuntimeGateway.RpcHandler.sync((request, sourceServer) -> sourceServer + ":" + request)
            )
        ) {
            client.publishToServer(topic, "server", new TestPayload("hello"));

            assertEquals(new TestPayload("hello"), received.get());
            assertEquals("client:ping", client.callServer("server", method, "ping").join());
        }
    }

    private record TestPayload(String value) {
    }
}
