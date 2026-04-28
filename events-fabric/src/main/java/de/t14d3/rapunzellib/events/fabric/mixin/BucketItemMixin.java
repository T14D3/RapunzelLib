package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to capture bucket operation events.
 * Dispatches BucketEmptyPre and BucketFillPre (both cancellable).
 */
@Mixin(BucketItem.class)
public class BucketItemMixin {

    @Inject(
            method = "use",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onBucketUsePre(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack stack = player.getItemInHand(hand);
        String bucketTypeKey = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        // #if VERSION >= 1.21.11
        String worldId = serverLevel.dimension().identifier().toString();
        // #else
        String worldId = serverLevel.dimension().location().toString();
        // #endif
        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);

        // Check if this is a bucket empty operation (bucket has content)
        boolean isEmpty = stack.is(Items.BUCKET);

        BlockPos pos = player.blockPosition();
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());

        if (!isEmpty) {
            // Bucket has content - this is an empty operation
            if (!bus.hasPreListeners(BucketEmptyPre.class)) return;

            BucketEmptyPre pre = new BucketEmptyPre(rPlayer, worldRef, rPos, bucketTypeKey);
            bus.dispatchPre(pre);

            if (pre.isDenied()) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        } else {
            // Bucket is empty - this is a fill operation
            if (!bus.hasPreListeners(BucketFillPre.class)) return;

            BlockState state = level.getBlockState(pos);
            String blockTypeKey = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

            BucketFillPre pre = new BucketFillPre(rPlayer, worldRef, rPos, blockTypeKey);
            bus.dispatchPre(pre);

            if (pre.isDenied()) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }
}
