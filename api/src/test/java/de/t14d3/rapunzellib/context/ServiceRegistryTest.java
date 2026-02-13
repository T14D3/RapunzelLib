package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.common.context.DefaultServiceRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServiceRegistryTest {
    @Test
    void registerLinkedResolvesAliasesToLatestPrimaryRegistration() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        GreetingServiceImpl first = new GreetingServiceImpl("first");
        GreetingServiceImpl second = new GreetingServiceImpl("second");

        registry.registerLinked(GreetingServiceImpl.class, first, GreetingService.class);
        registry.register(GreetingServiceImpl.class, second);

        assertSame(second, registry.get(GreetingService.class));
        assertSame(second, registry.get(GreetingServiceImpl.class));
        assertTrue(registry.serviceTypes().containsAll(List.of(GreetingService.class, GreetingServiceImpl.class)));
    }

    @Test
    void getOrCreateCanPopulateAliasTarget() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        registry.registerAlias(GreetingService.class, GreetingServiceImpl.class);

        GreetingService created = registry.getOrCreate(GreetingService.class, () -> new GreetingServiceImpl("created"));

        assertSame(created, registry.get(GreetingService.class));
        assertSame(created, registry.get(GreetingServiceImpl.class));
    }

    @Test
    void getOrCreateIsAtomicAcrossThreads() throws InterruptedException, ExecutionException {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        AtomicInteger createdCount = new AtomicInteger();

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            Callable<GreetingServiceImpl> task = () -> registry.getOrCreate(GreetingServiceImpl.class, () -> {
                createdCount.incrementAndGet();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
                return new GreetingServiceImpl("shared");
            });

            List<Future<GreetingServiceImpl>> futures = executor.invokeAll(List.of(task, task, task, task, task, task, task, task));
            GreetingServiceImpl shared = futures.getFirst().get();
            for (Future<GreetingServiceImpl> future : futures) {
                assertSame(shared, future.get());
            }
        }

        assertEquals(1, createdCount.get());
    }

    @Test
    void rejectsInvalidAliasConflictsAndCycles() {
        DefaultServiceRegistry registry = new DefaultServiceRegistry();
        registry.register(GreetingService.class, new GreetingServiceImpl("direct"));

        IllegalStateException conflict = assertThrows(
            IllegalStateException.class,
            () -> registry.registerAlias(GreetingService.class, GreetingServiceImpl.class)
        );
        IllegalStateException selfAlias = assertThrows(
            IllegalStateException.class,
            () -> registry.registerAlias(GreetingService.class, GreetingService.class)
        );

        assertTrue(conflict.getMessage().contains("direct service is already registered"));
        assertTrue(selfAlias.getMessage().contains("to itself"));
    }

    private interface GreetingService {
        String id();
    }

    private record GreetingServiceImpl(String id) implements GreetingService {
    }
}
