package de.t14d3.rapunzellib.common.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.context.DefaultRapunzelContext;
import de.t14d3.rapunzellib.common.message.YamlMessageFormatService;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;

public final class BootstrapServices {
    private BootstrapServices() {
    }

    public static DefaultRapunzelContext createContext(
        PlatformId platformId,
        Logger logger,
        Path dataDir,
        ResourceProvider resources,
        Scheduler scheduler
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataDir, "dataDir");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(scheduler, "scheduler");

        DefaultRapunzelContext ctx = new DefaultRapunzelContext(platformId, logger, dataDir, resources, scheduler);
        if (scheduler instanceof AutoCloseable closeable) {
            ctx.registerCloseable(closeable);
        }
        return ctx;
    }

    public static ConfigService registerYamlConfig(DefaultRapunzelContext ctx, ResourceProvider resources, Logger logger) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(logger, "logger");

        ConfigService configService = new SnakeYamlConfigService(resources, logger);
        ctx.register(ConfigService.class, configService);
        return configService;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static MessageFormatService registerYamlMessages(
        DefaultRapunzelContext ctx,
        ConfigService configService,
        Logger logger,
        Path dataDir
    ) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(configService, "configService");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataDir, "dataDir");

        MessageFormatService messageFormatService =
            new YamlMessageFormatService(configService, logger, dataDir.resolve("messages.yml"), "messages.yml");
        ctx.register(MessageFormatService.class, messageFormatService);
        return messageFormatService;
    }
}
