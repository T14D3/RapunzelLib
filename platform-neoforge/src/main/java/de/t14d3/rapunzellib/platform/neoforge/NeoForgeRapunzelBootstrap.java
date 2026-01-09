package de.t14d3.rapunzellib.platform.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.RapunzelLibVersion;
import de.t14d3.rapunzellib.common.context.DefaultRapunzelContext;
import de.t14d3.rapunzellib.common.message.YamlMessageFormatService;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.network.InMemoryMessenger;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.bootstrap.MessengerTransportBootstrap;
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.platform.neoforge.entity.NeoForgeBlocks;
import de.t14d3.rapunzellib.platform.neoforge.entity.NeoForgePlayers;
import de.t14d3.rapunzellib.platform.neoforge.entity.NeoForgeWorlds;
import de.t14d3.rapunzellib.platform.neoforge.network.NeoForgePluginMessenger;
import de.t14d3.rapunzellib.platform.neoforge.scheduler.NeoForgeScheduler;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class NeoForgeRapunzelBootstrap {
    public static final String MOD_ID = "rapunzellib_platform_neoforge";        

    private NeoForgeRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(
        String modId,
        MinecraftServer server,
        Logger logger,
        Path dataDirectory,
        Class<?> resourceAnchor
    ) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataDirectory, "dataDirectory");

        AtomicReference<RapunzelContext> created = new AtomicReference<>();
        Rapunzel.Lease lease = Rapunzel.bootstrapOrAcquire(modId, () -> {
            logger.info("Bootstrapping RapunzelLib {}", RapunzelLibVersion.current());

            try {
                Files.createDirectories(dataDirectory);
            } catch (Exception ignored) {
            }

            ResourceProvider resources = path -> Optional.ofNullable(openResource(resourceAnchor, path));
            Scheduler scheduler = new NeoForgeScheduler(server);

            DefaultRapunzelContext ctx = new DefaultRapunzelContext(
                PlatformId.NEOFORGE,
                logger,
                dataDirectory,
                resources,
                scheduler
            );
            created.set(ctx);
            ctx.registerCloseable((AutoCloseable) scheduler);

            ConfigService configService = new SnakeYamlConfigService(resources, logger);
            ctx.register(ConfigService.class, configService);

            MessageFormatService messageFormatService = new YamlMessageFormatService(
                configService,
                logger,
                dataDirectory.resolve("messages.yml"),
                "messages.yml"
            );
            ctx.register(MessageFormatService.class, messageFormatService);

            NeoForgePlayers players = new NeoForgePlayers(server);
            ctx.register(Players.class, players);
            ctx.register(NeoForgePlayers.class, players);

            NeoForgeWorlds worlds = new NeoForgeWorlds(server);
            ctx.register(Worlds.class, worlds);
            ctx.register(NeoForgeWorlds.class, worlds);

            NeoForgeBlocks blocks = new NeoForgeBlocks();
            ctx.register(Blocks.class, blocks);
            ctx.register(NeoForgeBlocks.class, blocks);

            InMemoryMessenger inMemoryMessenger = new InMemoryMessenger(modId, "velocity");
            Messenger messenger = inMemoryMessenger;
            ctx.register(Messenger.class, messenger);
            ctx.register(InMemoryMessenger.class, inMemoryMessenger);

            // Optional transport: Redis (recommended for NeoForge backend usage).
            try {
                var transportConfig = configService.load(dataDirectory.resolve("config.yml"), "config.yml");
                String transport = transportConfig.getString("network.transport", "plugin");
                switch (transport.trim().toLowerCase()) {
                    case "redis" -> {
                        var result = MessengerTransportBootstrap.bootstrap(
                            transportConfig,
                            PlatformId.NEOFORGE,
                            logger,
                            ctx.services()
                        );
                        messenger = result.messenger();
                        ctx.services().register(Messenger.class, messenger);
                        ctx.registerCloseable(result.closeable());
                    }
                    case "plugin" -> {
                        NeoForgePluginMessenger pluginMessenger = new NeoForgePluginMessenger(server, logger);
                        messenger = pluginMessenger;
                        ctx.services().register(Messenger.class, messenger);
                        ctx.register(NeoForgePluginMessenger.class, pluginMessenger);
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
                    ctx.registerCloseable(networkInfo);

                    if (messenger instanceof NeoForgePluginMessenger pluginMessenger) {
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
