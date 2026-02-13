package de.t14d3.rapunzellib.common.objects.interop;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.objects.interop.RNativeInterop;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultRNativeInteropTest {
    @AfterEach
    void tearDown() {
        Rapunzel.shutdownAll();
    }

    @Test
    void bootstrapRegistersLinkedInteropServiceAndRejectsPlatformMismatch(@TempDir Path dir) {
        PlatformRuntime runtime = BootstrapServices.serverRuntime(PlatformId.PAPER, de.t14d3.rapunzellib.runtime.EngineFamily.MOJANG_SERVER, this);
        BootstrapServices.FirstPhaseResult firstPhase = BootstrapServices.bootstrapFirstPhase(
            runtime,
            LoggerFactory.getLogger(DefaultRNativeInteropTest.class),
            dir,
            path -> Optional.empty(),
            new InlineScheduler()
        );

        RNativeInterop interop = firstPhase.context().services().get(RNativeInterop.class);
        MutableRNativeInterop mutableInterop = firstPhase.context().services().get(MutableRNativeInterop.class);
        assertSame(interop, mutableInterop);
        assertSame(interop, firstPhase.context().nativeInterop().orElseThrow());

        mutableInterop.registerViewAdapter(TestNative.class, SecondaryView.class, nativeWrapper ->
            Optional.of(new SecondaryView(nativeWrapper.handle(String.class).toUpperCase()))
        );

        Rapunzel.bootstrap(this, firstPhase.context());

        TestNative paperWrapper = new TestNative(PlatformId.PAPER, "paper");
        TestNative spongeWrapper = new TestNative(PlatformId.SPONGE, "sponge");

        assertEquals("PAPER", paperWrapper.handle(SecondaryView.class).value());
        assertEquals(Optional.of("PAPER"), paperWrapper.tryHandle(SecondaryView.class).map(SecondaryView::value));
        assertTrue(spongeWrapper.tryHandle(SecondaryView.class).isEmpty());
        assertThrows(ClassCastException.class, () -> spongeWrapper.handle(SecondaryView.class));
    }

    private record SecondaryView(String value) {
    }

    private static final class TestNative extends RNativeHandle<String> {
        private TestNative(@NotNull PlatformId platformId, @NotNull String handle) {
            super(platformId, handle);
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
