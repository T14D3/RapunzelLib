package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class BucketEntityMixin {
    @Unique
    private static final String INTERACT_LIVING_ENTITY =
        "Lnet/minecraft/world/item/ItemStack;interactLivingEntity(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;";

    @Inject(
        method = "interactOn",
        at = @At(value = "INVOKE", target = INTERACT_LIVING_ENTITY),
        cancellable = true
    )
    private void onBucketEntityPre(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!(entity instanceof LivingEntity livingEntity)) return;
        if (!((Object) this instanceof ServerPlayer serverPlayer)) return;
        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) return;

        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (!(stack.getItem() instanceof BucketItem)) return;
        if (!bus.hasPreListeners(BucketEntityPre.class)) return;

        String worldId = serverLevel.dimension().identifier().toString();
        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);

        BlockPos pos = livingEntity.blockPosition();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RKey entityTypeKey = RKey.of(BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType()).toString());

        BucketEntityPre pre = new BucketEntityPre(rPlayer, worldRef, rPos, entityTypeKey);
        bus.dispatchPre(pre);

        if (pre.isDenied()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
