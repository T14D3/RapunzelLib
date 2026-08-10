package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerLoginPre;
import de.t14d3.rapunzellib.nbt.shared.SharedAdventureComponentCodec;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Shared Fabric/NeoForge bridge for {@link PlayerLoginPre} (HEAD) and
 * {@link PlayerJoinPost} (RETURN), injecting into
 * {@code PlayerList.placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)}
 * - the single choke point through which every player join flows in 26.1.2
 * (identical in 1.21.10/1.21.11/26.2).
 *
 * <p>Pre at HEAD is cancellable: denying kicks the player by disconnecting the
 * connection with the deny reason (mirroring Paper's
 * {@code PlayerLoginEvent.disallow(KICK_OTHER, reason)} semantics) and aborts
 * the join before any packet is sent. The payload carries the live
 * {@link RPlayer} (the {@code ServerPlayer} exists at this point), matching
 * the Paper bridge.</p>
 *
 * <p>Join Post at RETURN fires exactly once, after the join processing
 * completed; a cancelled HEAD never reaches RETURN, so the Post never fires
 * for denied logins. The NeoForge {@code PlayerLoggedInEvent} (also fired
 * inside {@code placeNewPlayer}) is deliberately NOT subscribed - this mixin
 * is the single Post source on both platforms, avoiding double fire.</p>
 */
@Mixin(PlayerList.class)
public abstract class PlayerLoginMixin {

    @Inject(method = "placeNewPlayer", at = @At("HEAD"), cancellable = true)
    private void onPlayerLoginPre(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
                                  CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(PlayerLoginPre.class)) return;

        PlayerLoginPre pre = new PlayerLoginPre(
            player.getGameProfile().name(),
            player.getUUID(),
            Rapunzel.players().require(player),
            false
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            // The deny reason is an adventure component (the shared event API);
            // disconnect() needs the vanilla component.
            connection.disconnect(SharedAdventureComponentCodec.toNative(
                pre.denyReason().orElse(Component.text("Disconnected"))
            ));
            ci.cancel();
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void onPlayerJoinPost(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
                                  CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPostListeners(PlayerJoinPost.class)) return;

        UUID uuid = player.getUUID();
        String name = player.getGameProfile().name();
        bus.dispatchPost(new PlayerJoinPost(uuid, name));
    }
}
