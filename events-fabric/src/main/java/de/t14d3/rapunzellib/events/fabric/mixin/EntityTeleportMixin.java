package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.EntityTeleportPost;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTeleportMixin {
    @Inject(method = "teleportTo", at = @At("RETURN"))
    private void onEntityTeleportPost(double x, double y, double z, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!bus.hasPostListeners(EntityTeleportPost.class)) return;

        Entity self = (Entity) (Object) this;
        Level level = self.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        var rEntity = Rapunzel.entities().require(self);
        if (rEntity == null) return;

        // #if VERSION >= 1.21.11
        String worldId = serverLevel.dimension().identifier().toString();
        // #else
        String worldId = serverLevel.dimension().location().toString();
        // #endif
        RWorldRef worldRef = new RWorldRef(worldId, worldId);

        RLocation to = new RLocation(worldRef, x, y, z, self.getYRot(), self.getXRot());

        // "From" location is approximated from the entity's previous position
        RLocation from = new RLocation(worldRef, self.xo, self.yo, self.zo, self.yRotO, self.xRotO);

        bus.dispatchPost(new EntityTeleportPost(rEntity, from, to));
    }
}
