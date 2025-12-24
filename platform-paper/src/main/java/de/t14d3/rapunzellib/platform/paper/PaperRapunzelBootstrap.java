package de.t14d3.rapunzellib.platform.paper;

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
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class PaperRapunzelBootstrap {
    private PaperRapunzelBootstrap() {
    }

    public static RapunzelContext bootstrap(JavaPlugin plugin) {
        Logger logger = plugin.getSLF4JLogger();
        Path dataDir = plugin.getDataFolder().toPath();
        ResourceProvider resources = path -> Optional.ofNullable(openResource(plugin, path));
        Scheduler scheduler = new PaperScheduler(plugin);

        DefaultRapunzelContext ctx = new DefaultRapunzelContext(PlatformId.PAPER, logger, dataDir, resources, scheduler);

        ConfigService configService = new SnakeYamlConfigService(resources, logger);
        ctx.register(ConfigService.class, configService);

        MessageFormatService messageFormatService = new YamlMessageFormatService(configService, logger, dataDir.resolve("messages.yml"), "messages.yml");
        ctx.register(MessageFormatService.class, messageFormatService);

        PaperPlayers players = new PaperPlayers();
        ctx.register(Players.class, players);
        ctx.register(PaperPlayers.class, players);

        PaperWorlds worlds = new PaperWorlds();
        ctx.register(Worlds.class, worlds);
        ctx.register(PaperWorlds.class, worlds);

        PaperBlocks blocks = new PaperBlocks();
        ctx.register(Blocks.class, blocks);
        ctx.register(PaperBlocks.class, blocks);

        PaperPluginMessenger messenger = new PaperPluginMessenger(plugin);      
        ctx.register(Messenger.class, messenger);
        ctx.register(PaperPluginMessenger.class, messenger);
        ctx.registerCloseable(messenger);

        NetworkInfoClient networkInfo = new NetworkInfoClient(messenger, scheduler, logger);
        ctx.register(NetworkInfoService.class, networkInfo);
        ctx.register(NetworkInfoClient.class, networkInfo);

        networkInfo.networkServerName()
            .thenAccept(messenger::setNetworkServerName)
            .exceptionally(ignored -> null);

        AtomicReference<ScheduledTask> resolveNameTask = new AtomicReference<>();
        resolveNameTask.set(scheduler.runRepeating(Duration.ofSeconds(1), Duration.ofSeconds(5), () -> {
            if (messenger.hasNetworkServerName()) {
                ScheduledTask task = resolveNameTask.get();
                if (task != null) task.cancel();
                return;
            }
            if (!messenger.isConnected()) return;
            networkInfo.networkServerName()
                .thenAccept(messenger::setNetworkServerName)
                .exceptionally(ignored -> null);
        }));
        ctx.registerCloseable(() -> {
            ScheduledTask task = resolveNameTask.get();
            if (task != null) task.cancel();
        });

        Rapunzel.bootstrap(plugin, ctx);
        return ctx;
    }

    private static InputStream openResource(JavaPlugin plugin, String path) {
        if (path == null) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return plugin.getResource(normalized);
    }
}
