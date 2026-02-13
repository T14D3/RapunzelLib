package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.common.context.DefaultServiceRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.objects.interop.RNativeInterop;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RNativeInteropAccessTest {
    @AfterEach
    void tearDown() {
        Rapunzel.shutdownAll();
    }

    @Test
    void directPrimaryHandleAccessStaysAvailableWithoutInterop() {
        String primaryHandle = "primary";
        TestNative nativeWrapper = new TestNative(PlatformId.PAPER, primaryHandle);

        assertSame(primaryHandle, nativeWrapper.handle());
        assertSame(primaryHandle, nativeWrapper.handle(String.class));
        assertEquals(Optional.of(primaryHandle), nativeWrapper.tryHandle(String.class));
    }

    @Test
    void derivedTypedViewLookupUsesRegisteredInterop(@TempDir Path dir) {
        TestMutableInterop interop = new TestMutableInterop();
        interop.registerViewAdapter(TestNative.class, SecondaryView.class, nativeWrapper ->
            Optional.of(new SecondaryView(nativeWrapper.handle(String.class).length()))
        );

        TestContext context = new TestContext(dir);
        context.services().register(MutableRNativeInterop.class, interop);
        context.services().register(RNativeInterop.class, interop);
        Rapunzel.bootstrap(this, context);

        TestNative nativeWrapper = new TestNative(PlatformId.PAPER, "paper");

        SecondaryView view = nativeWrapper.handle(SecondaryView.class);
        assertEquals(5, view.size());
        assertEquals(Optional.of(5), nativeWrapper.tryHandle(SecondaryView.class).map(SecondaryView::size));
    }

    @Test
    void missingBootstrapFallsBackToPrimaryHandleOnly() {
        TestNative nativeWrapper = new TestNative(PlatformId.PAPER, "paper");

        assertTrue(nativeWrapper.tryHandle(SecondaryView.class).isEmpty());
        assertThrows(ClassCastException.class, () -> nativeWrapper.handle(SecondaryView.class));
    }

    private record SecondaryView(int size) {
    }

    private static final class TestNative extends RNativeHandle<String> {
        private TestNative(@NotNull PlatformId platformId, @NotNull String handle) {
            super(platformId, handle);
        }
    }

    private static final class TestMutableInterop implements MutableRNativeInterop {
        private final Map<Key, de.t14d3.rapunzellib.objects.interop.RNativeViewAdapter<?, ?>> adapters = new ConcurrentHashMap<>();

        @Override
        public <N extends RNative, T> void registerViewAdapter(
            @NotNull Class<N> nativeType,
            @NotNull Class<T> viewType,
            @NotNull de.t14d3.rapunzellib.objects.interop.RNativeViewAdapter<? super N, T> adapter
        ) {
            adapters.put(Key.type(nativeType, viewType), adapter);
        }

        @Override
        public <T> @NotNull Optional<T> findView(@NotNull RNative nativeWrapper, @NotNull Class<T> type) {
            return Optional.ofNullable(adapters.get(Key.type(nativeWrapper.getClass(), type)))
                .map(RNativeInteropAccessTest.TestMutableInterop::castAdapter)
                .flatMap(adapter -> adapter.findView(nativeWrapper))
                .map(type::cast);
        }

        @SuppressWarnings("unchecked")
        private static de.t14d3.rapunzellib.objects.interop.RNativeViewAdapter<RNative, Object> castAdapter(Object adapter) {
            return (de.t14d3.rapunzellib.objects.interop.RNativeViewAdapter<RNative, Object>) adapter;
        }

        private record Key(Class<?> nativeType, Class<?> viewType) {
            private static Key type(Class<?> nativeType, Class<?> viewType) {
                return new Key(nativeType, viewType);
            }
        }
    }

    private static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

        private final Path dataDirectory;
        private final PlatformRuntime runtime = new PlatformRuntime(
            PlatformId.PAPER,
            RuntimeRole.SERVER,
            EngineFamily.MOJANG_SERVER,
            EnumSet.noneOf(de.t14d3.rapunzellib.runtime.RuntimeCapability.class),
            new LifecycleOwner(this)
        );
        private final DefaultServiceRegistry services = new DefaultServiceRegistry();

        private TestContext(Path dataDirectory) {
            this.dataDirectory = dataDirectory;
        }

        @Override
        public @NotNull PlatformRuntime runtime() {
            return runtime;
        }

        @Override
        public @NotNull Logger logger() {
            return LOGGER;
        }

        @Override
        public @NotNull Path dataDirectory() {
            return dataDirectory;
        }

        @Override
        public @NotNull ResourceProvider resources() {
            return _path -> Optional.empty();
        }

        @Override
        public @NotNull Scheduler scheduler() {
            return new InlineScheduler();
        }

        @Override
        public @NotNull ServiceRegistry services() {
            return services;
        }
    }

    private static final class InlineScheduler implements Scheduler {
        @Override
        public @NotNull ScheduledTask run(@NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }

        @Override
        public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }

        @Override
        public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }

        @Override
        public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            task.run();
            return NoopTask.INSTANCE;
        }
    }

    private enum NoopTask implements ScheduledTask {
        INSTANCE;

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
