package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.remote.rpc.ProxyServiceMethods;
import de.t14d3.rapunzellib.network.remote.rpc.Requests;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntimeGateway;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * Consumes deferred cross-server teleports on backends.
 *
 * <p>When the proxy connects a player to this server with a destination
 * location, that location is stored on the proxy. Once the player has joined,
 * this listener polls the proxy for the pending location and applies it. The
 * poll is deliberately delayed so the join sequence is not blocked, and the
 * actual teleport is executed on the main thread after the RPC response
 * arrives on a network thread.</p>
 *
 * <p>Installed only when a network gateway exists; without networking (or with
 * only the in-memory transport) the RPC fails fast or times out and is handled
 * as a debug-level no-op, leaving RapunzelLib behaviour unchanged.</p>
 */
public final class DeferredTeleportJoinListener implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(DeferredTeleportJoinListener.class);
    private static final Duration POLL_DELAY = Duration.ofSeconds(1);

    private final NetworkRuntimeGateway gateway;
    private final Scheduler scheduler;

    private DeferredTeleportJoinListener(@NotNull NetworkRuntimeGateway gateway, @NotNull Scheduler scheduler) {
        this.gateway = gateway;
        this.scheduler = scheduler;
    }

    /**
     * Registers the listener on the platform plugin when a network gateway is
     * available. No-op when networking is absent.
     */
    public static void install(@NotNull JavaPlugin platformPlugin) {
        Objects.requireNonNull(platformPlugin, "platformPlugin");
        Rapunzel.findContext().ifPresent(context ->
            context.services().find(NetworkRuntimeGateway.class).ifPresent(gateway -> {
                DeferredTeleportJoinListener listener =
                    new DeferredTeleportJoinListener(gateway, context.scheduler());
                platformPlugin.getServer().getPluginManager().registerEvents(listener, platformPlugin);
                logger.debug("[Remote] Deferred teleport join listener installed (gateway={})",
                    gateway.runtime().localName());
            })
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Do not block the join sequence; poll shortly after the player is in.
        scheduler.runLater(POLL_DELAY, () -> pollDeferredTeleport(player));
    }

    private void pollDeferredTeleport(Player player) {
        gateway.callProxy(
                ProxyServiceMethods.PROXY_POLL_DEFERRED_TELEPORT,
                new Requests.PollDeferredTeleportRequest(player.getUniqueId())
            )
            .whenComplete((result, error) -> {
                if (error != null) {
                    // No proxy reachable / handler absent: nothing to consume.
                    logger.debug("Deferred teleport poll for {} failed: {}",
                        player.getUniqueId(), error.getMessage());
                    return;
                }
                if (result == null || result.location() == null) {
                    return;
                }
                RLocation location = result.location();
                // The RPC completes on a network thread; teleport on the main thread.
                scheduler.run(() -> teleport(player, location));
            });
    }

    private void teleport(Player player, RLocation location) {
        if (!player.isOnline()) {
            return;
        }
        World world = location.world().name() != null
            ? Bukkit.getWorld(location.world().name())
            : Bukkit.getWorld(location.world().identifier());
        if (world == null) {
            logger.debug("Deferred teleport for {} skipped: world {} is not loaded",
                player.getUniqueId(), location.world().identifier());
            return;
        }
        try {
            player.teleport(new Location(
                world, location.x(), location.y(), location.z(),
                location.yaw(), location.pitch()));
            logger.info("[Remote] Deferred teleport applied for {} to {}",
                player.getName(), location.world().identifier());
        } catch (Exception e) {
            logger.debug("Deferred teleport for {} failed: {}", player.getUniqueId(), e.getMessage());
        }
    }
}
