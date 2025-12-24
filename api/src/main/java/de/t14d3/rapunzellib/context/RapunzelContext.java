package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.nio.file.Path;

public interface RapunzelContext extends AutoCloseable {
    PlatformId platformId();

    Logger logger();

    Path dataDirectory();

    ResourceProvider resources();

    Scheduler scheduler();

    ServiceRegistry services();

    default ConfigService configs() {
        return services().get(ConfigService.class);
    }

    default MessageFormatService messages() {
        return services().get(MessageFormatService.class);
    }

    default Players players() {
        return services().get(Players.class);
    }

    default Worlds worlds() {
        return services().get(Worlds.class);
    }

    default Blocks blocks() {
        return services().get(Blocks.class);
    }

    @Override
    default void close() throws Exception {
        // no-op
    }
}
