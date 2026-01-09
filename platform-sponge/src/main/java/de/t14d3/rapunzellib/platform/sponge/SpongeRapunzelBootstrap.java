package de.t14d3.rapunzellib.platform.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
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
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongeBlocks;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongePlayers;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongeWorlds;
import de.t14d3.rapunzellib.platform.sponge.scheduler.SpongeScheduler;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.plugin.PluginContainer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class SpongeRapunzelBootstrap {
    private SpongeRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(
            PluginContainer container,
            Path dataDirectory,
            Server server
    ) {
        if (container == null) throw new IllegalArgumentException("container cannot be null");

        Object plugin = container.instance();
        String pluginId = container.metadata().id();
        Logger logger = LoggerFactory.getLogger(pluginId);
        Class<?> resourceAnchor = plugin.getClass();

        try {
            Files.createDirectories(dataDirectory);
        } catch (Exception ignored) {
        }

        ResourceProvider resources = path -> Optional.ofNullable(openResource(resourceAnchor, path));

        Scheduler scheduler = new SpongeScheduler(server);

        DefaultRapunzelContext ctx = new DefaultRapunzelContext(
            PlatformId.SPONGE,
            logger,
            dataDirectory,
            resources,
            scheduler
        );
        ctx.registerCloseable((AutoCloseable) scheduler);

        ctx.register(Server.class, server);

        ConfigService configService = new SnakeYamlConfigService(resources, logger);
        ctx.register(ConfigService.class, configService);

        MessageFormatService messageFormatService = new YamlMessageFormatService(
            configService,
            logger,
            dataDirectory.resolve("messages.yml"),
            "messages.yml"
        );
        ctx.register(MessageFormatService.class, messageFormatService);

        SpongePlayers players = new SpongePlayers();
        ctx.register(Players.class, players);
        ctx.register(SpongePlayers.class, players);

        SpongeWorlds worlds = new SpongeWorlds();
        ctx.register(Worlds.class, worlds);
        ctx.register(SpongeWorlds.class, worlds);

        SpongeBlocks blocks = new SpongeBlocks();
        ctx.register(Blocks.class, blocks);
        ctx.register(SpongeBlocks.class, blocks);

        InMemoryMessenger inMemoryMessenger = new InMemoryMessenger(pluginId, "velocity");
        ctx.register(Messenger.class, inMemoryMessenger);
        ctx.register(InMemoryMessenger.class, inMemoryMessenger);

        try {
            var transportConfig = configService.load(dataDirectory.resolve("config.yml"), "config.yml");
            var result = MessengerTransportBootstrap.bootstrap(transportConfig, PlatformId.SPONGE, logger, ctx.services());
            ctx.registerCloseable(result.closeable());
        } catch (Exception e) {
            logger.warn("Failed to initialize network transport; using in-memory. Reason: {}", e.getMessage());
            ctx.services().register(Messenger.class, inMemoryMessenger);
        }

        Rapunzel.bootstrap(plugin, ctx);
        return ctx;
    }

    private static InputStream openResource(Class<?> anchor, String path) {
        if (anchor == null || path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = anchor.getClassLoader();
        return (cl != null) ? cl.getResourceAsStream(normalized) : null;
    }
}
