package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;

import de.t14d3.rapunzellib.events.player.PlayerMovePost;
import de.t14d3.rapunzellib.events.player.PlayerMovePre;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches {@link PlayerMovePre} (cancellable) and {@link PlayerMovePost}
 * on player movement via {@code Entity.move()}.
 */
@Mixin(Entity.class)
public abstract class PlayerMoveMixin {

    @Unique
    private static final Map<UUID, Vec3> LAST_POSITIONS = new ConcurrentHashMap<>();
    @Unique
    private static final double THRESHOLD = 1.0;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onPlayerMovePre(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer player)) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(PlayerMovePre.class)) return;

        Vec3 from = self.position();
        Vec3 to = from.add(movement);

        // Suppress noise below threshold
        Vec3 last = LAST_POSITIONS.get(self.getUUID());
        if (last != null) {
            if (Math.abs(to.x - last.x) < THRESHOLD
                    && Math.abs(to.y - last.y) < THRESHOLD
                    && Math.abs(to.z - last.z) < THRESHOLD) {
                return;
            }
        }

        RLocation rFrom = makeLocation(serverLevel, from, self.getYRot(), self.getXRot());
        RLocation rTo = makeLocation(serverLevel, to, self.getYRot(), self.getXRot());

        PlayerMovePre pre = new PlayerMovePre(Rapunzel.players().require(player), rFrom, rTo);
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            ci.cancel();
        } else {
            // Record from so post know where we moved from
            LAST_POSITIONS.put(self.getUUID(), from);
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void onPlayerMovePost(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer player)) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPostListeners(PlayerMovePost.class)) return;

        Vec3 from = LAST_POSITIONS.remove(self.getUUID());
        if (from == null) return;

        Vec3 to = self.position();
        RLocation rFrom = makeLocation(serverLevel, from, player.yRotO, player.xRotO);
        RLocation rTo = makeLocation(serverLevel, to, player.getYRot(), player.getXRot());

        bus.dispatchPost(new PlayerMovePost(Rapunzel.players().require(player), rFrom, rTo, false));
    }

    @Unique
    private static RLocation makeLocation(ServerLevel level, Vec3 pos, float yaw, float pitch) {
        return new RLocation(worldRef(level), pos.x, pos.y, pos.z, yaw, pitch);
    }

    @Unique
    private static RWorldRef worldRef(ServerLevel level) {
        // #if VERSION >= 1.21.11
        RKey key = RKey.of(level.dimension().identifier().toString());
        // #else
        RKey key = RKey.of(level.dimension().location().toString());
        // #endif
        return new RWorldRef(null, key);
    }
}