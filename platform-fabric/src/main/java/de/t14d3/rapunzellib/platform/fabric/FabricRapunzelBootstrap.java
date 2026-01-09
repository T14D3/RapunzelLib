package de.t14d3.rapunzellib.platform.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.RapunzelLibVersion;
import de.t14d3.rapunzellib.common.context.DefaultRapunzelContext;
import de.t14d3.rapunzellib.common.message.YamlMessageFormatService;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.network.InMemoryMessenger;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.bootstrap.MessengerTransportBootstrap;
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricBlocks;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricPlayers;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricWorlds;
import de.t14d3.rapunzellib.platform.fabric.network.FabricPluginMessenger;
import de.t14d3.rapunzellib.platform.fabric.scheduler.FabricScheduler;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class FabricRapunzelBootstrap {
    private FabricRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(String modId, MinecraftServer server, Class<?> resourceAnchor) {
        Logger logger = LoggerFactory.getLogger(modId);

        AtomicReference<RapunzelContext> created = new AtomicReference<>();
        Rapunzel.Lease lease = Rapunzel.bootstrapOrAcquire(modId, () -> {
            logger.info("Bootstrapping RapunzelLib {}", RapunzelLibVersion.current());

            Path dataDir = FabricLoader.getInstance().getConfigDir().resolve(modId);
            try {
                Files.createDirectories(dataDir);
            } catch (Exception ignored) {
            }

            ResourceProvider resources = path -> Optional.ofNullable(openResource(resourceAnchor, path));
            Scheduler scheduler = new FabricScheduler(server);
            DefaultRapunzelContext ctx = new DefaultRapunzelContext(PlatformId.FABRIC, logger, dataDir, resources, scheduler);
            created.set(ctx);

            AutoCloseable closeable = (AutoCloseable) scheduler;
            ctx.registerCloseable(closeable);

            ConfigService configService = new SnakeYamlConfigService(resources, logger);
            ctx.register(ConfigService.class, configService);

            MessageFormatService messageFormatService =
                new YamlMessageFormatService(configService, logger, dataDir.resolve("messages.yml"), "messages.yml");
            ctx.register(MessageFormatService.class, messageFormatService);

            FabricPlayers players = new FabricPlayers(server);
            ctx.register(Players.class, players);
            ctx.register(FabricPlayers.class, players);

            FabricWorlds worlds = new FabricWorlds(server);
            ctx.register(Worlds.class, worlds);
            ctx.register(FabricWorlds.class, worlds);

            FabricBlocks blocks = new FabricBlocks();
            ctx.register(Blocks.class, blocks);
            ctx.register(FabricBlocks.class, blocks);

            InMemoryMessenger inMemoryMessenger = new InMemoryMessenger(modId, "velocity");
            Messenger messenger = inMemoryMessenger;
            ctx.register(Messenger.class, messenger);
            ctx.register(InMemoryMessenger.class, inMemoryMessenger);

            // Optional transport: plugin messaging (proxy) or Redis.
            try {
                var transportConfig = configService.load(dataDir.resolve("config.yml"), "config.yml");
                String transport = transportConfig.getString("network.transport", "plugin");
                switch (transport.trim().toLowerCase()) {
                    case "redis" -> {
                        var result = MessengerTransportBootstrap.bootstrap(transportConfig, PlatformId.FABRIC, logger, ctx.services());
                        messenger = result.messenger();
                        ctx.services().register(Messenger.class, messenger);
                        ctx.registerCloseable(result.closeable());
                    }
                    case "plugin" -> {
                        FabricPluginMessenger pluginMessenger = new FabricPluginMessenger(server, logger);
                        messenger = pluginMessenger;
                        ctx.services().register(Messenger.class, messenger);
                        ctx.register(FabricPluginMessenger.class, pluginMessenger);
                        ctx.registerCloseable(pluginMessenger);
                    }
                    default -> {
                        // keep in-memory
                    }
                }

                // When we have a real transport, expose network info client.
                if (!(messenger instanceof InMemoryMessenger)) {
                    NetworkInfoClient networkInfo = new NetworkInfoClient(messenger, scheduler, logger);
                    ctx.register(NetworkInfoService.class, networkInfo);
                    ctx.register(NetworkInfoClient.class, networkInfo);

                    if (messenger instanceof FabricPluginMessenger pluginMessenger) {
                        networkInfo.networkServerName()
                            .thenAccept(pluginMessenger::setNetworkServerName)
                            .exceptionally(ignored -> null);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to initialize network transport; using in-memory. Reason: {}", e.getMessage());
                ctx.services().register(Messenger.class, inMemoryMessenger);
            }

            return ctx;
        });

        if (created.get() == null) {
            logger.debug("RapunzelLib already bootstrapped; acquiring lease for {}", modId);
        }
        return lease.context();
    }

    private static InputStream openResource(Class<?> anchor, String path) {
        if (anchor == null || path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = anchor.getClassLoader();
        return (cl != null) ? cl.getResourceAsStream(normalized) : null;
    }
}
