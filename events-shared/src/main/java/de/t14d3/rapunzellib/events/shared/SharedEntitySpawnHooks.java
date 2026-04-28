package de.t14d3.rapunzellib.events.shared;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityEventPayloads;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPost;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SharedEntitySpawnHooks {
    private SharedEntitySpawnHooks() {
    }

    public static boolean dispatchSpawnPre(
        @NotNull GameEventBus bus,
        @NotNull Entity entity,
        @NotNull String reason,
        boolean cancelled
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(reason, "reason");
        if (!bus.hasPreListeners(EntitySpawnPre.class)) {
            return cancelled;
        }

        EntitySpawnPre pre = new EntitySpawnPre(worldRef(entity.level()), pos(entity), typeKey(entity), reason, cancelled);
        bus.dispatchPre(pre);
        return pre.isDenied();
    }

    public static void dispatchCancelledSpawnSnapshot(
        @NotNull GameEventBus bus,
        @NotNull Entity entity,
        @NotNull String reason
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(reason, "reason");
        if (!bus.hasAsyncListeners(EntitySpawnSnapshot.class)) {
            return;
        }

        bus.dispatchAsync(new EntitySpawnSnapshot(
            REntitySnapshot.of(entity.getUUID(), worldRef(entity.level()), pos(entity), typeKey(entity)),
            reason,
            true
        ));
    }

    public static void dispatchSpawnOutcome(
        @NotNull GameEventBus bus,
        @NotNull Entity entity,
        @NotNull String reason,
        boolean cancelled
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(reason, "reason");
        boolean needsPost = bus.hasPostListeners(EntitySpawnPost.class);
        boolean needsAsync = bus.hasAsyncListeners(EntitySpawnSnapshot.class);
        if (!needsPost && !needsAsync) {
            return;
        }

        var rEntity = Rapunzel.entities().require(entity);
        if (needsPost && !cancelled) {
            bus.dispatchPost(EntityEventPayloads.spawnPost(rEntity, reason, false));
        }
        if (needsAsync) {
            bus.dispatchAsync(EntityEventPayloads.spawnSnapshot(rEntity, reason, cancelled));
        }
    }

    private static @NotNull RWorldRef worldRef(@NotNull Level level) {
        // #if VERSION >= 1.21.11
        String worldId = level.dimension().identifier().toString();
        // #else
        String worldId = level.dimension().location().toString();
        // #endif
        return new RWorldRef(worldId, worldId);
    }

    private static @NotNull RBlockPos pos(@NotNull Entity entity) {
        BlockPos pos = entity.blockPosition();
        return new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private static @NotNull RKey typeKey(@NotNull Entity entity) {
        return RKey.of(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
}
