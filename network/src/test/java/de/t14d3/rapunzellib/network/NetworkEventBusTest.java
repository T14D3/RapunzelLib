package de.t14d3.rapunzellib.network;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NetworkEventBusTest {
    private record Payload(int x) {
    }

    @Test
    void typedListenersReceiveMessagesAndUnsubscribe() {
        InMemoryMessenger messenger = new InMemoryMessenger("a", "velocity");
        NetworkEventBus bus = new NetworkEventBus(messenger);

        AtomicInteger called = new AtomicInteger();
        NetworkEventBus.Subscription sub = bus.register("ch", Payload.class, (_p, _src) -> called.incrementAndGet());

        bus.sendToAll("ch", new Payload(1));
        assertEquals(1, called.get());

        sub.close();
        bus.sendToAll("ch", new Payload(2));
        assertEquals(1, called.get());
    }
}

