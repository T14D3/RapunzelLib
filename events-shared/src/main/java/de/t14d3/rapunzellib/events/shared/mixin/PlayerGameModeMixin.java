package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerStatePost;
import de.t14d3.rapunzellib.objects.RGameMode;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Fabric-only bridge for the {@link PlayerStatePost} GAMEMODE updates,
 * injecting into {@code ServerPlayerGameMode.changeGameModeForPlayer(GameType)}
 * at RETURN - the method that actually applies a game-mode change in 26.1.2
 * (identical in 1.21.10/1.21.11/26.2); every change path (the
 * {@code /gamemode} command, F3+N, hardcore death -> spectator) funnels
 * through it.
 *
 * <p>The Post fires only when the method returned {@code true} (an actual
 * change was applied - the method returns {@code false} when the mode did not
 * change), and the snapshot is built from the player's live state plus the
 * new mode, mirroring the Paper bridge's {@code PlayerGameModeChangeEvent}
 * payload. NeoForge uses its native {@code PlayerChangeGameModeEvent}
 * instead; this mixin must NOT be registered there to avoid double fire.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerGameModeMixin {

    @Shadow
    protected ServerPlayer player;

    @Inject(method = "changeGameModeForPlayer", at = @At("RETURN"))
    private void onPlayerGameModeChange(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        // False = the requested mode equals the current one; no change, no Post.
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPostListeners(PlayerStatePost.class)) return;

        RPlayer rPlayer = Rapunzel.players().require(player);

        PlayerStatePost.PlayerStateSnapshot snapshot = new PlayerStatePost.PlayerStateSnapshot(
            RGameMode.valueOf(gameType.name()),
            player.isShiftKeyDown(),
            player.getAbilities().flying,
            player.isSprinting(),
            player.getVehicle() != null
        );
        bus.dispatchPost(new PlayerStatePost(rPlayer, snapshot, Set.of(PlayerStatePost.StateField.GAMEMODE)));
    }
}
