package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockEventDispatchUtilTest {
    @Test
    @Disabled("BlockEventDispatchUtil now resolves RBlockType/RWorld via registries; requires runtime platform initialization")
    void dispatchBlockFormPreUsesTypedKeys() {
        GameEventBus bus = new GameEventBus(new InlineScheduler(), LoggerFactory.getLogger(BlockEventDispatchUtilTest.class));
        AtomicReference<BlockFormPre> captured = new AtomicReference<>();
        bus.onPre(BlockFormPre.class, captured::set);

        boolean denied = BlockEventDispatchUtil.dispatchBlockFormPre(
            bus,
            RKey.of("minecraft:overworld"),
            1,
            64,
            2,
            RKey.of("minecraft:ice"),
            RKey.of("minecraft:water")
        );

        assertFalse(denied);
        assertEquals(RKey.of("minecraft:overworld"), captured.get().world().key());
        assertEquals(RKey.of("minecraft:ice"), captured.get().newTypeKey());
        assertEquals(RKey.of("minecraft:water"), captured.get().sourceTypeKey());
    }

    @Test
    @Disabled("BlockEventDispatchUtil now resolves RBlockType/RWorld via registries; requires runtime platform initialization")
    void dispatchBlockPhysicsPostUsesTypedKeys() {
        GameEventBus bus = new GameEventBus(new InlineScheduler(), LoggerFactory.getLogger(BlockEventDispatchUtilTest.class));
        AtomicReference<BlockPhysicsPost> captured = new AtomicReference<>();
        bus.onPost(BlockPhysicsPost.class, captured::set);

        BlockEventDispatchUtil.dispatchBlockPhysicsPost(
            bus,
            RKey.of("minecraft:the_nether"),
            4,
            70,
            8,
            RKey.of("minecraft:redstone_wire"),
            RKey.of("minecraft:redstone_wire"),
            true
        );

        assertTrue(captured.get().cancelled());
        assertEquals(RKey.of("minecraft:the_nether"), captured.get().world().key());
        assertEquals(RKey.of("minecraft:redstone_wire"), captured.get().blockTypeKey());
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
