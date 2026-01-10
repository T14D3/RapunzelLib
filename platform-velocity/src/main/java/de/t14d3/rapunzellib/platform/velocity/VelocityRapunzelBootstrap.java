package de.t14d3.rapunzellib.platform.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.RapunzelLibVersion;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.common.context.DefaultRapunzelContext;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.network.InMemoryMessenger;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.bootstrap.MessengerTransportBootstrap;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.queue.NetworkQueueBootstrap;
import de.t14d3.rapunzellib.platform.velocity.objects.VelocityBlocks;
import de.t14d3.rapunzellib.platform.velocity.objects.VelocityPlayers;
import de.t14d3.rapunzellib.platform.velocity.objects.VelocityWorlds;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityPluginMessenger;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityNetworkInfoResponder;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityNetworkInfoService;
import de.t14d3.rapunzellib.platform.velocity.scheduler.VelocityScheduler;      
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class VelocityRapunzelBootstrap {
    private VelocityRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        AtomicReference<RapunzelContext> created = new AtomicReference<>();
        Rapunzel.Lease lease = Rapunzel.bootstrapOrAcquire(plugin, () -> {
            logger.info("Bootstrapping RapunzelLib {}", RapunzelLibVersion.current());

            ResourceProvider resources = path -> Optional.ofNullable(openResource(plugin, path));
            Scheduler scheduler = new VelocityScheduler(proxy, plugin);

            DefaultRapunzelContext ctx =
                BootstrapServices.createContext(PlatformId.VELOCITY, logger, dataDirectory, resources, scheduler);
            created.set(ctx);

            ConfigService configService = BootstrapServices.registerYamlConfig(ctx, resources, logger);
            BootstrapServices.registerYamlMessages(ctx, configService, logger, dataDirectory);

            VelocityPlayers players = new VelocityPlayers(proxy);
            ctx.register(Players.class, players);
            ctx.register(VelocityPlayers.class, players);

            VelocityWorlds worlds = new VelocityWorlds();
            ctx.register(Worlds.class, worlds);
            ctx.register(VelocityWorlds.class, worlds);

            VelocityBlocks blocks = new VelocityBlocks();
            ctx.register(Blocks.class, blocks);
            ctx.register(VelocityBlocks.class, blocks);

            InMemoryMessenger inMemory = new InMemoryMessenger("velocity", "velocity");
            ctx.register(Messenger.class, inMemory);
            ctx.register(InMemoryMessenger.class, inMemory);

            Messenger messenger = inMemory;
            try {
                var transportConfig = configService.load(dataDirectory.resolve("config.yml"), "config.yml");
                String transport = transportConfig.getString("network.transport", "plugin");
                String normalized = (transport != null) ? transport.trim().toLowerCase(Locale.ROOT) : "plugin";

                switch (normalized) {
                    case "redis" -> {
                        var result = MessengerTransportBootstrap.bootstrap(
                            transportConfig,
                            PlatformId.VELOCITY,
                            logger,
                            ctx.services()
                        );
                        messenger = result.messenger();
                        ctx.registerCloseable(result.closeable());
                    }
                    case "plugin" -> {
                        VelocityPluginMessenger pluginMessenger = new VelocityPluginMessenger(plugin, proxy, logger);
                        Messenger effective = pluginMessenger;

                        String ownerId = PlatformId.VELOCITY.name() + ":" + dataDirectory.toAbsolutePath().normalize();
                        NetworkQueueBootstrap.Result queued = NetworkQueueBootstrap.wrapIfEnabled(
                            pluginMessenger,
                            transportConfig,
                            scheduler,
                            logger,
                            ownerId,
                            () -> proxy.getAllServers().stream()
                                .map(rs -> rs.getServerInfo().getName())
                                .toList(),
                            targetServer -> {
                                if (targetServer == null || targetServer.isBlank()) return false;
                                String t = targetServer.trim();
                                return proxy.getAllPlayers().stream()
                                    .anyMatch(p -> p.getCurrentServer()
                                        .map(sc -> sc.getServerInfo().getName().equalsIgnoreCase(t))
                                        .orElse(false));
                            },
                            null
                        );
                        effective = queued.messenger();

                        ctx.register(Messenger.class, effective);
                        ctx.register(VelocityPluginMessenger.class, pluginMessenger);
                        if (effective != pluginMessenger) {
                            pluginMessenger.setUndeliverableForwarder(effective);
                        }
                        messenger = effective;
                    }
                    default -> {
                        // keep in-memory
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to initialize network transport; using in-memory.", e);
                ctx.register(Messenger.class, inMemory);
            }

            VelocityNetworkInfoResponder networkInfoResponder = new VelocityNetworkInfoResponder(messenger, proxy, logger);
            ctx.register(VelocityNetworkInfoResponder.class, networkInfoResponder);

            VelocityNetworkInfoService networkInfo = new VelocityNetworkInfoService(messenger, proxy);
            ctx.register(NetworkInfoService.class, networkInfo);
            ctx.register(VelocityNetworkInfoService.class, networkInfo);        

            return ctx;
        });

        if (created.get() == null) {
            logger.debug("RapunzelLib already bootstrapped; acquiring lease for {}", plugin.getClass().getName());
        }
        return lease.context();
    }

    private static InputStream openResource(Object plugin, String path) {
        if (path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = plugin.getClass().getClassLoader();
        return cl.getResourceAsStream(normalized);
    }
}
