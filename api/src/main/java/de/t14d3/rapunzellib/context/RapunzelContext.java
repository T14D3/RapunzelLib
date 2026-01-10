package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;

public interface RapunzelContext extends AutoCloseable {
    @NotNull PlatformId platformId();

    @NotNull Logger logger();

    @NotNull Path dataDirectory();

    @NotNull ResourceProvider resources();

    @NotNull Scheduler scheduler();

    @NotNull ServiceRegistry services();

    /**
     * Registers a service into this context.
     *
     * <p>Default implementation registers into {@link #services()} only. Context
     * implementations may override to provide additional lifecycle tracking (e.g. auto-closing).</p>
     */
    default <T> @NotNull T register(@NotNull Class<T> type, @NotNull T instance) {
        services().register(type, instance);
        if (instance instanceof AutoCloseable closeable) {
            registerCloseable(closeable);
        }
        return instance;
    }

    /**
     * Registers a closeable to be closed when the context shuts down.
     *
     * <p>Default implementation is a no-op. Implementations with lifecycle tracking
     * should override.</p>
     */
    default void registerCloseable(@NotNull AutoCloseable closeable) {
        // no-op by default
    }

    default @NotNull ConfigService configs() {
        return services().get(ConfigService.class);
    }

    default @NotNull MessageFormatService messages() {
        return services().get(MessageFormatService.class);
    }

    default @NotNull Players players() {
        return services().get(Players.class);
    }

    default @NotNull Worlds worlds() {
        return services().get(Worlds.class);
    }

    default @NotNull Blocks blocks() {
        return services().get(Blocks.class);
    }

    @Override
    default void close() throws Exception {
        // no-op
    }
}
