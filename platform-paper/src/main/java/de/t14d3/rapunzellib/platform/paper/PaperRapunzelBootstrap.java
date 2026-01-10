package de.t14d3.rapunzellib.platform.paper;

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
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.queue.NetworkQueueBootstrap;
import de.t14d3.rapunzellib.platform.paper.objects.PaperBlocks;
import de.t14d3.rapunzellib.platform.paper.objects.PaperPlayers;
import de.t14d3.rapunzellib.platform.paper.objects.PaperWorlds;
import de.t14d3.rapunzellib.platform.paper.network.PaperPluginMessenger;
import de.t14d3.rapunzellib.platform.paper.scheduler.PaperScheduler;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.InputStream;
import java.time.Duration;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class PaperRapunzelBootstrap {
    private PaperRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(JavaPlugin plugin) {
        Logger logger = plugin.getSLF4JLogger();

        AtomicReference<RapunzelContext> created = new AtomicReference<>();
        Rapunzel.Lease lease = Rapunzel.bootstrapOrAcquire(plugin, () -> {
            logger.info("Bootstrapping RapunzelLib {}", RapunzelLibVersion.current());

            Path dataDir = plugin.getDataFolder().toPath();
            ResourceProvider resources = path -> Optional.ofNullable(openResource(plugin, path));
            Scheduler scheduler = new PaperScheduler(plugin);

            DefaultRapunzelContext ctx =
                BootstrapServices.createContext(PlatformId.PAPER, logger, dataDir, resources, scheduler);
            created.set(ctx);

            ConfigService configService = BootstrapServices.registerYamlConfig(ctx, resources, logger);
            BootstrapServices.registerYamlMessages(ctx, configService, logger, dataDir);

            PaperPlayers players = new PaperPlayers();
            ctx.register(Players.class, players);
            ctx.register(PaperPlayers.class, players);

            PaperWorlds worlds = new PaperWorlds();
            ctx.register(Worlds.class, worlds);
            ctx.register(PaperWorlds.class, worlds);

            PaperBlocks blocks = new PaperBlocks();
            ctx.register(Blocks.class, blocks);
            ctx.register(PaperBlocks.class, blocks);

            InMemoryMessenger inMemoryMessenger = new InMemoryMessenger(plugin.getName(), "velocity");
            ctx.register(Messenger.class, inMemoryMessenger);
            ctx.register(InMemoryMessenger.class, inMemoryMessenger);

            try {
                var transportConfig = configService.load(dataDir.resolve("config.yml"), "config.yml");
                String transport = transportConfig.getString("network.transport", "plugin");
                String normalized = (transport != null) ? transport.trim().toLowerCase(Locale.ROOT) : "plugin";

                switch (normalized) {
                    case "redis" -> {
                        var result = MessengerTransportBootstrap.bootstrap(
                            transportConfig,
                            PlatformId.PAPER,
                            logger,
                            ctx.services()
                        );
                        ctx.registerCloseable(result.closeable());
                    }
                    case "plugin" -> {
                        PaperPluginMessenger pluginMessenger = new PaperPluginMessenger(plugin);

                        Messenger effectiveMessenger = pluginMessenger;
                        String ownerId = PlatformId.PAPER.name() + ":" + dataDir.toAbsolutePath().normalize();
                        NetworkQueueBootstrap.Result queued = NetworkQueueBootstrap.wrapIfEnabled(
                            pluginMessenger,
                            transportConfig,
                            scheduler,
                            logger,
                            ownerId
                        );
                        effectiveMessenger = queued.messenger();

                        if (effectiveMessenger == pluginMessenger) {
                            ctx.register(Messenger.class, pluginMessenger);
                            ctx.services().register(PaperPluginMessenger.class, pluginMessenger);
                        } else {
                            ctx.register(Messenger.class, effectiveMessenger);
                            ctx.register(PaperPluginMessenger.class, pluginMessenger);
                        }

                        NetworkInfoClient networkInfo = new NetworkInfoClient(pluginMessenger, scheduler, logger);
                        ctx.register(NetworkInfoService.class, networkInfo);    
                        ctx.register(NetworkInfoClient.class, networkInfo);     

                        networkInfo.networkServerName()
                            .thenAccept(pluginMessenger::setNetworkServerName)
                            .exceptionally(ignored -> null);

                        AtomicReference<ScheduledTask> resolveNameTask = new AtomicReference<>();
                        resolveNameTask.set(scheduler.runRepeating(Duration.ofSeconds(1), Duration.ofSeconds(5), () -> {
                            if (pluginMessenger.hasNetworkServerName()) {
                                ScheduledTask task = resolveNameTask.get();
                                if (task != null) task.cancel();
                                return;
                            }
                            if (!pluginMessenger.isConnected()) return;
                            networkInfo.networkServerName()
                                .thenAccept(pluginMessenger::setNetworkServerName)
                                .exceptionally(ignored -> null);
                        }));
                        ctx.registerCloseable(() -> {
                            ScheduledTask task = resolveNameTask.get();
                            if (task != null) task.cancel();
                        });
                    }
                    default -> {
                        // keep in-memory
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to initialize network transport; using in-memory.", e);
                ctx.register(Messenger.class, inMemoryMessenger);
            }

            return ctx;
        });

        if (created.get() == null) {
            logger.debug("RapunzelLib already bootstrapped; acquiring lease for {}", plugin.getName());
        }
        return lease.context();
    }

    private static InputStream openResource(JavaPlugin plugin, String path) {
        if (path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return plugin.getResource(normalized);
    }
}
