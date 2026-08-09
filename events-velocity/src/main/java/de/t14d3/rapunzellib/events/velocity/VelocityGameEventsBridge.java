package de.t14d3.rapunzellib.events.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Velocity proxy bridge for the RapunzelLib game event bus.
 *
 * <p>Dispatches {@link PlayerJoinPost} from {@code PostLoginEvent} and
 * {@link PlayerQuitPost} from {@code DisconnectEvent}, giving backend
 * consumers a single proxy-aware join/quit stream (the same payload records
 * the Paper bridge uses). The proxy has no notion of the game-server events
 * in the catalog (blocks, entities, inventories, chat, ...), so those are
 * deliberately not bridged.</p>
 */
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

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        proxy.getEventManager().unregisterListener(plugin, this);
    }
}
