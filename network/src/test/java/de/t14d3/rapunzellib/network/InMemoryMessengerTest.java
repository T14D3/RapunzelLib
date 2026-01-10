package de.t14d3.rapunzellib.network;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InMemoryMessengerTest {
    @Test
    void sendToServerOnlyDeliversToMatchingTarget() {
        InMemoryMessenger messenger = new InMemoryMessenger("a", "velocity");
        AtomicInteger deliveries = new AtomicInteger();

        messenger.registerListener("ch", (_ch, _data, _src) -> deliveries.incrementAndGet());

        messenger.sendToServer("ch", "b", "x");
        assertEquals(0, deliveries.get());

        messenger.sendToServer("ch", "a", "x");
        assertEquals(1, deliveries.get());
    }

    @Test
    void sendToProxyOnlyDeliversWhenLocalIsProxy() {
        AtomicInteger deliveries = new AtomicInteger();

        InMemoryMessenger backend = new InMemoryMessenger("a", "velocity");
        backend.registerListener("ch", (_ch, _data, _src) -> deliveries.incrementAndGet());
        backend.sendToProxy("ch", "x");
        assertEquals(0, deliveries.get());

        InMemoryMessenger proxy = new InMemoryMessenger("velocity", "velocity");
        proxy.registerListener("ch", (_ch, _data, _src) -> deliveries.incrementAndGet());
        proxy.sendToProxy("ch", "x");
        assertEquals(1, deliveries.get());
    }
}
