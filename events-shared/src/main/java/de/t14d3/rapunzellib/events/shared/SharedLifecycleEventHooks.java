package de.t14d3.rapunzellib.events.shared;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.world.ChunkUnloadPost;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Shared hooks for dispatching lifecycle events (world load, chunk unload, player quit). */
public final class SharedLifecycleEventHooks {
    private SharedLifecycleEventHooks() {
    }

    public static void initializeMixins(@NotNull GameEventBus bus) {
        SharedMixinEventsBridge.initialize(Objects.requireNonNull(bus, "bus"));
    }

    public static void dispatchWorldLoadPost(@NotNull GameEventBus bus, @NotNull ServerLevel level) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        if (!bus.hasPostListeners(WorldLoadPost.class)) {
            return;
        }
        bus.dispatchPost(new WorldLoadPost(worldRef(level)));
    }

    public static void dispatchChunkUnloadPost(
        @NotNull GameEventBus bus,
        @NotNull ServerLevel level,
        int chunkX,
        int chunkZ
    ) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(level, "level");
        if (!bus.hasPostListeners(ChunkUnloadPost.class)) {
            return;
        }
        bus.dispatchPost(new ChunkUnloadPost(worldRef(level), chunkX, chunkZ));
    }

    public static void dispatchPlayerQuitPost(@NotNull GameEventBus bus, @NotNull ServerPlayer player) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(player, "player");
        if (!bus.hasPostListeners(PlayerQuitPost.class)) {
            return;
        }
        bus.dispatchPost(new PlayerQuitPost(player.getUUID(), player.getName().getString()));
    }

    private static @NotNull RWorldRef worldRef(@NotNull ServerLevel level) {
        // #if VERSION >= 1.21.11
        RKey id = RKey.of(level.dimension().identifier().toString());
        // #else
        RKey id = RKey.of(level.dimension().location().toString());
        // #endif
        return new RWorldRef(null, id);
    }
}
