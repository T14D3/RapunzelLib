package de.t14d3.rapunzellib.events.velocity;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerMessagePost;
import de.t14d3.rapunzellib.events.player.PlayerMessagePre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Velocity proxy bridge for the RapunzelLib game event bus.
 *
 * <p>Dispatches {@link PlayerJoinPost} from {@code PostLoginEvent},
 * {@link PlayerQuitPost} from {@code DisconnectEvent}, and
 * {@link PlayerMessagePre}/{@link PlayerMessagePost} from the proxy's
 * {@code PlayerChatEvent} (chat, {@code isCommand=false}) and
 * {@code CommandExecuteEvent} (commands, {@code isCommand=true}), giving
 * backend consumers a proxy-aware join/quit/message stream with the same
 * payload records the Paper bridge uses.</p>
 *
 * <p>Proxy caveats: the chat message is the raw client text as seen by the
 * proxy (it may be unsigned - the payload carries no signature), and the
 * Pre/Post pair is dispatched from the same proxy event (Pre first, Post last
 * when the message was allowed through), mirroring the Paper bridge's
 * "pipeline processed, outcome decided" Post semantics.</p>
 *
 * <p>Deprecation notes (velocity-api 3.4.0-SNAPSHOT): {@code @Subscribe.order}
 * is deprecated in favour of the raw {@code priority} short and the typed
 * {@code setResult(ChatResult)} overload is deprecated - both still work and
 * the typed/readable forms are kept here (see the suppression below).</p>
 */
@SuppressWarnings("deprecation")
public final class VelocityGameEventsBridge implements GameEventBridge {
    private final GameEventBus bus;
    private final ProxyServer proxy;
    private final Object plugin;
    private boolean closed;

    VelocityGameEventsBridge(
        @NotNull GameEventBus bus,
        @NotNull ProxyServer proxy,
        @NotNull Object plugin
    ) {
        this.bus = Objects.requireNonNull(bus, "bus");
        this.proxy = Objects.requireNonNull(proxy, "proxy");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        proxy.getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!bus.hasPostListeners(PlayerJoinPost.class)) return;
        bus.dispatchPost(new PlayerJoinPost(
            event.getPlayer().getUniqueId(),
            event.getPlayer().getUsername()
        ));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (!bus.hasPostListeners(PlayerQuitPost.class)) return;
        bus.dispatchPost(new PlayerQuitPost(
            event.getPlayer().getUniqueId(),
            event.getPlayer().getUsername()
        ));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChatPre(PlayerChatEvent event) {
        if (!bus.hasPreListeners(PlayerMessagePre.class)) return;

        PlayerMessagePre pre = new PlayerMessagePre(
            Rapunzel.players().require(event.getPlayer()),
            event.getMessage(),
            false
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChatPost(PlayerChatEvent event) {
        if (!bus.hasPostListeners(PlayerMessagePost.class)) return;
        if (!event.getResult().isAllowed()) return;

        bus.dispatchPost(new PlayerMessagePost(
            Rapunzel.players().require(event.getPlayer()),
            event.getMessage(),
            false,
            false
        ));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onCommandExecutePre(CommandExecuteEvent event) {
        if (!bus.hasPreListeners(PlayerMessagePre.class)) return;
        if (!(event.getCommandSource() instanceof Player player)) return;

        PlayerMessagePre pre = new PlayerMessagePre(
            Rapunzel.players().require(player),
            event.getCommand(),
            true
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onCommandExecutePost(CommandExecuteEvent event) {
        if (!bus.hasPostListeners(PlayerMessagePost.class)) return;
        if (!event.getResult().isAllowed()) return;
        if (!(event.getCommandSource() instanceof Player player)) return;

        bus.dispatchPost(new PlayerMessagePost(
            Rapunzel.players().require(player),
            event.getCommand(),
            true,
            false
        ));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        proxy.getEventManager().unregisterListener(plugin, this);
    }
}
