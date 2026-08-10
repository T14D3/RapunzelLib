package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.events.shared.mixin.SharedBlockMixinHooks;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dispatches {@link InteractBlockPre} with kind {@link InteractBlockPre.Action#STEP}
 * when a player collides with a pressure plate, mirroring Paper's
 * {@code PlayerInteractEvent} {@code Action.PHYSICAL} for plates.
 *
 * <p>Fires from {@link BasePressurePlateBlock#entityInside} at the same point
 * Paper fires the PHYSICAL event (entity stepping onto the plate while it is
 * unpressed). Cancelling short-circuits {@code checkPressed}, so the plate
 * does NOT activate - same effect as cancelling the Paper event.</p>
 */
@Mixin(BasePressurePlateBlock.class)
public abstract class PressurePlateStepMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onPlayerStep(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effectApplier,
        boolean isPrecise,
        CallbackInfo ci
    ) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;

        // Paper only fires the PHYSICAL event when the plate would turn ON,
        // i.e. it is currently unpressed. Mirror that: stepping on an already
        // pressed plate does not dispatch.
        BlockState current = level.getBlockState(pos);
        boolean pressed = current.hasProperty(PressurePlateBlock.POWERED)
            ? current.getValue(PressurePlateBlock.POWERED)
            : current.getValue(WeightedPressurePlateBlock.POWER) > 0;
        if (pressed) return;

        if (SharedBlockMixinHooks.dispatchInteractBlockStepPre(bus, serverLevel, pos, player)) {
            ci.cancel();
        }
    }
}
