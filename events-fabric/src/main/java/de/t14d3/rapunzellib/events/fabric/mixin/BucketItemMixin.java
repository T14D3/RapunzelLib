package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.objects.*;
import de.t14d3.rapunzellib.objects.block.RBlock;

import de.t14d3.rapunzellib.registry.RItemType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    /**
     * Empty bucket -> filling from a fluid source.
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onBucketFillPre(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        BucketItem self = (BucketItem) (Object) this;

        if (self.getContent() != Fluids.EMPTY) {
            return;
        }

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(BucketFillPre.class)) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockHitResult hit = getPlayerPOVHitResultCustom(
                level,
                player,
                ClipContext.Fluid.SOURCE_ONLY
        );

        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = hit.getBlockPos();

        RWorld world = Rapunzel.worlds().require(serverLevel);
        RBlockPos rPos = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        RBlock block = Rapunzel.blocks().at(world, rPos);
        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);

        BucketFillPre event = new BucketFillPre(rPlayer, block);
        bus.dispatchPre(event);

        if (event.isDenied()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    private static BlockHitResult getPlayerPOVHitResultCustom(Level level, Player player, ClipContext.Fluid fluid) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        return level.clip(new ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.OUTLINE, fluid, player));
    }

    /**
     * Filled bucket -> placing fluid.
     */
    @Inject(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void onBucketEmptyPre(
            Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, ItemStack itemStack, BlockHitResult hitResult, BlockPos pos, Direction direction
    ) {
        BucketItem self = (BucketItem) (Object) this;
        BlockPos directionOffsetPos = pos.relative(direction);

        if (self.getContent() == Fluids.EMPTY) {
            return;
        }

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(BucketEmptyPre.class)) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        /*
         * Vanilla immediately computes the actual placement position after this.
         * Reuse that exact logic so future vanilla changes here only require
         * updating this one line.
         */
        BlockState clicked = level.getBlockState(pos);
        BlockPos placePos =
                clicked.getBlock() instanceof net.minecraft.world.level.block.LiquidBlockContainer
                        && self.getContent() == Fluids.WATER
                        ? pos
                        : directionOffsetPos;


        var key = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        RItemType bucketTypeKey = RItemType.require(RKey.of(key.getNamespace(), key.getPath()));

        // #if VERSION >= 1.21.11
        String worldId = serverLevel.dimension().identifier().toString();
        // #else
        String worldId = serverLevel.dimension().location().toString();
        // #endif

        RWorldRef worldRef = new RWorldRef(worldId, worldId);
        RPlayer rPlayer = Rapunzel.players().require(serverPlayer);
        RLocation location = new RLocation(worldRef, placePos.getX(), placePos.getY(), placePos.getZ());

        RBlockPos rPos = new RBlockPos(
                placePos.getX(),
                placePos.getY(),
                placePos.getZ()
        );

        BucketEmptyPre event = new BucketEmptyPre(rPlayer, location, bucketTypeKey, cir.isCancelled());

        bus.dispatchPre(event);

        if (event.isDenied()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}