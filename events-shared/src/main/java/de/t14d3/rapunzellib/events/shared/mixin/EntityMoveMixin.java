package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityMovePost;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Entity.class)
public abstract class EntityMoveMixin {
    private static final Map<UUID, Vec3> LAST_POSITIONS = new ConcurrentHashMap<>();
    private static final double THRESHOLD = 1.0;

    @Inject(method = "move", at = @At("RETURN"))
    private void onEntityMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Level level = self.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPostListeners(EntityMovePost.class)) return;

        UUID uuid = self.getUUID();
        Vec3 current = self.position();
        Vec3 previous = LAST_POSITIONS.get(uuid);

        // Skip if movement is below threshold
        if (previous != null) {
            if (Math.abs(current.x - previous.x) < THRESHOLD
                    && Math.abs(current.y - previous.y) < THRESHOLD
                    && Math.abs(current.z - previous.z) < THRESHOLD) {
                return;
            }
        }

        LAST_POSITIONS.put(uuid, current);

        var rEntity = Rapunzel.entities().require(self);
        // #if VERSION >= 1.21.11
        RKey dimKey = RKey.of(serverLevel.dimension().identifier().toString());
        // #else
        RKey dimKey = RKey.of(serverLevel.dimension().location().toString());
        // #endif
        RWorldRef worldRef = new RWorldRef(null, dimKey);
        Vec3 from = previous != null ? previous : current;

        bus.dispatchPost(new EntityMovePost(rEntity,
                new RLocation(worldRef, from.x, from.y, from.z),
                new RLocation(worldRef, current.x, current.y, current.z)));
    }
}