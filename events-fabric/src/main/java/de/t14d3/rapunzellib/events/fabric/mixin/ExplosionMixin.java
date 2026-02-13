package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to capture explosion events.
 * Dispatches ExplosionPre (cancellable with block list modification).
 */
@Mixin(ServerExplosion.class)
public class ExplosionMixin {

    @Inject(
        method = "explode",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onExplosionPre(CallbackInfoReturnable<Integer> cir, List<BlockPos> affectedBlockPositions) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        if (!bus.hasPreListeners(ExplosionPre.class)) return;

        ServerExplosion explosion = (ServerExplosion) (Object) this;
        ServerLevel level = explosion.level();

        // Get explosion center
        BlockPos center = BlockPos.containing(explosion.center());
        String worldId = level.dimension().identifier().toString();
        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        RBlockPos origin = new RBlockPos(center.getX(), center.getY(), center.getZ());

        // Determine source type
        String sourceTypeKey = "unknown";
        Entity source = explosion.getDirectSourceEntity();
        if (source != null) {
            sourceTypeKey = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()).toString();
        }

        List<RBlockPos> affectedBlocks = new ArrayList<>(affectedBlockPositions.size());
        for (BlockPos pos : affectedBlockPositions) {
            affectedBlocks.add(new RBlockPos(pos.getX(), pos.getY(), pos.getZ()));
        }

        ExplosionPre pre = new ExplosionPre(worldRef, origin, sourceTypeKey, affectedBlocks);
        bus.dispatchPre(pre);

        if (pre.isDenied()) {
            cir.setReturnValue(0);
            return;
        }

        affectedBlockPositions.clear();
        for (RBlockPos pos : pre.affectedBlocks()) {
            affectedBlockPositions.add(new BlockPos(pos.x(), pos.y(), pos.z()));
        }
    }
}
