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
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dispatches {@link InteractBlockPre} with kind {@link InteractBlockPre.Action#STEP}
 * when a player collides with a tripwire, mirroring Paper's
 * {@code PlayerInteractEvent} {@code Action.PHYSICAL} for tripwires.
 *
 * <p>{@link TripWireBlock} does not extend {@link BasePressurePlateBlock}, so
 * this is a separate mixin. Fires from {@link TripWireBlock#entityInside};
 * cancelling short-circuits {@code checkPressed}, so the tripwire does NOT
 * arm - same effect as cancelling the Paper event. Like Paper, only attached
 * tripwires dispatch (a lone string has no tripwire circuit).</p>
 */
@Mixin(TripWireBlock.class)
public abstract class TripWireStepMixin {
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

        // Paper only fires the PHYSICAL event for attached tripwires that are
        // about to be armed (vanilla entityInside already only proceeds while
        // the wire is unpowered; mirror the ATTACHED guard explicitly).
        if (state.getValue(TripWireBlock.POWERED)) return;
        if (!state.getValue(TripWireBlock.ATTACHED)) return;

        if (SharedBlockMixinHooks.dispatchInteractBlockStepPre(bus, serverLevel, pos, player)) {
            ci.cancel();
        }
    }
}
