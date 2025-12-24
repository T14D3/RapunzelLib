package de.t14d3.rapunzellib.platform.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
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
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
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
import java.util.Optional;

public final class VelocityRapunzelBootstrap {
    private VelocityRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(Object plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        ResourceProvider resources = path -> Optional.ofNullable(openResource(plugin, path));
        Scheduler scheduler = new VelocityScheduler(proxy, plugin);

        DefaultRapunzelContext ctx = new DefaultRapunzelContext(PlatformId.VELOCITY, logger, dataDirectory, resources, scheduler);

        ConfigService configService = new SnakeYamlConfigService(resources, logger);
        ctx.register(ConfigService.class, configService);

        MessageFormatService messageFormatService = new YamlMessageFormatService(configService, logger, dataDirectory.resolve("messages.yml"), "messages.yml");
        ctx.register(MessageFormatService.class, messageFormatService);

        VelocityPlayers players = new VelocityPlayers(proxy);
        ctx.register(Players.class, players);
        ctx.register(VelocityPlayers.class, players);

        VelocityWorlds worlds = new VelocityWorlds();
        ctx.register(Worlds.class, worlds);
        ctx.register(VelocityWorlds.class, worlds);

        VelocityBlocks blocks = new VelocityBlocks();
        ctx.register(Blocks.class, blocks);
        ctx.register(VelocityBlocks.class, blocks);

        VelocityPluginMessenger messenger = new VelocityPluginMessenger(plugin, proxy, logger);
        ctx.register(Messenger.class, messenger);
        ctx.register(VelocityPluginMessenger.class, messenger);
        ctx.registerCloseable(messenger);

        VelocityNetworkInfoResponder networkInfoResponder = new VelocityNetworkInfoResponder(messenger, proxy, logger);
        ctx.register(VelocityNetworkInfoResponder.class, networkInfoResponder);
        ctx.registerCloseable(networkInfoResponder);

        VelocityNetworkInfoService networkInfo = new VelocityNetworkInfoService(messenger, proxy);
        ctx.register(NetworkInfoService.class, networkInfo);
        ctx.register(VelocityNetworkInfoService.class, networkInfo);

        Rapunzel.bootstrap(plugin, ctx);
        return ctx;
    }

    private static InputStream openResource(Object plugin, String path) {
        if (path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = plugin.getClass().getClassLoader();
        return cl.getResourceAsStream(normalized);
    }
}
