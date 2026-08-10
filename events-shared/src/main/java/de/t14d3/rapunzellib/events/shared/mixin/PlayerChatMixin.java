package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerMessagePost;
import de.t14d3.rapunzellib.events.player.PlayerMessagePre;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric-only bridge for {@link PlayerMessagePre} / {@link PlayerMessagePost},
 * injecting into the vanilla chat/command packet handlers in 26.1.2
 * (identical in 1.21.10/1.21.11/26.2):
 * {@code ServerGamePacketListenerImpl.handleChat} (chat messages) and
 * {@code handleChatCommand} + {@code handleSignedChatCommand} (commands,
 * both the unsigned and the signed-arguments packet variants).
 *
 * <p>Pre at HEAD is cancellable: denying drops the message/command before it
 * reaches the chat pipeline or the command dispatcher (nothing is broadcast,
 * no other handler sees it - the same deny semantics as Paper's
 * {@code AsyncChatEvent}/{@code PlayerCommandPreprocessEvent}). Post at
 * RETURN fires only when the Pre was not denied (a cancelled HEAD never
 * reaches RETURN), mirroring Paper's "pipeline processed, outcome decided"
 * Post semantics.</p>
 *
 * <p>{@link PlayerMessagePre#isCommand()} mirrors Paper's computation:
 * chat packets -> {@code false}, command packets -> {@code true}. Command
 * content includes the leading {@code '/'} exactly like Paper's
 * {@code PlayerCommandPreprocessEvent.getMessage()} (the packet carries the
 * command without the slash).</p>
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerChatMixin {

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void onPlayerMessagePreChat(ServerboundChatPacket packet, CallbackInfo ci) {
        dispatchPre(packet.message(), false, ci);
    }

    @Inject(method = "handleChat", at = @At("RETURN"))
    private void onPlayerMessagePostChat(ServerboundChatPacket packet, CallbackInfo ci) {
        dispatchPost(packet.message(), false);
    }

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void onPlayerMessagePreCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        dispatchPre("/" + packet.command(), true, ci);
    }

    @Inject(method = "handleChatCommand", at = @At("RETURN"))
    private void onPlayerMessagePostCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        dispatchPost("/" + packet.command(), true);
    }

    @Inject(method = "handleSignedChatCommand", at = @At("HEAD"), cancellable = true)
    private void onPlayerMessagePreSignedCommand(ServerboundChatCommandSignedPacket packet, CallbackInfo ci) {
        dispatchPre("/" + packet.command(), true, ci);
    }

    @Inject(method = "handleSignedChatCommand", at = @At("RETURN"))
    private void onPlayerMessagePostSignedCommand(ServerboundChatCommandSignedPacket packet, CallbackInfo ci) {
        dispatchPost("/" + packet.command(), true);
    }

    private void dispatchPre(String content, boolean isCommand, CallbackInfo ci) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(PlayerMessagePre.class)) return;
        if (content == null) return;

        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).getPlayer();
        PlayerMessagePre pre = new PlayerMessagePre(Rapunzel.players().require(player), content, isCommand);
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            ci.cancel();
        }
    }

    private void dispatchPost(String content, boolean isCommand) {
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPostListeners(PlayerMessagePost.class)) return;
        if (content == null) return;

        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).getPlayer();
        bus.dispatchPost(new PlayerMessagePost(Rapunzel.players().require(player), content, isCommand, false));
    }
}
