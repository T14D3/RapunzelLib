package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class BucketEntityMixin {

    @Inject(
        method = "interactOn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;interactLivingEntity(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"),
        cancellable = true
    )
    private void onBucketEntityPre(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!(entity instanceof LivingEntity livingEntity)) return;
        if (!((Object) this instanceof ServerPlayer serverPlayer)) return;
        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) return;

        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (!(stack.getItem() instanceof BucketItem)) return;
        if (!bus.hasPreListeners(BucketEntityPre.class)) return;

        // #if VERSION >= 1.21.11
        String worldId = serverLevel.dimension().identifier().toString();
        // #else
        String worldId = serverLevel.dimension().location().toString();
        // #endif
        RWorldRef worldRef = new RWorldRef(null, worldId);
        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);

        RLocation rLocation = new RLocation(worldRef, location.x(), location.y(), location.z());
        var rEntity = Rapunzel.entities().require(livingEntity);

        BucketEntityPre pre = new BucketEntityPre(rPlayer, rLocation, rEntity, cir.isCancelled());
        bus.dispatchPre(pre);

        if (pre.isDenied()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
