package de.t14d3.rapunzellib.common.context;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

public final class DefaultRapunzelContext implements RapunzelContext {
    private final PlatformId platformId;
    private final Logger logger;
    private final Path dataDirectory;
    private final ResourceProvider resources;
    private final Scheduler scheduler;
    private final DefaultServiceRegistry services = new DefaultServiceRegistry();

    private final List<AutoCloseable> closeables = new ArrayList<>();
    private final IdentityHashMap<AutoCloseable, Boolean> closeableSet = new IdentityHashMap<>();

    public DefaultRapunzelContext(
        PlatformId platformId,
        Logger logger,
        Path dataDirectory,
        ResourceProvider resources,
        Scheduler scheduler
    ) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public @NotNull PlatformId platformId() {
        return platformId;
    }

    @Override
    public @NotNull Logger logger() {
        return logger;
    }

    @Override
    public @NotNull Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public @NotNull ResourceProvider resources() {
        return resources;
    }

    @Override
    public @NotNull Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public @NotNull ServiceRegistry services() {
        return services;
    }

    @Override
    public <T> @NotNull T register(@NotNull Class<T> type, @NotNull T instance) {
        services.register(type, instance);
        if (instance instanceof AutoCloseable closeable) {
            registerCloseable(closeable);
        }
        return instance;
    }

    @Override
    public void registerCloseable(@NotNull AutoCloseable closeable) {
        AutoCloseable c = Objects.requireNonNull(closeable, "closeable");
        if (closeableSet.putIfAbsent(c, Boolean.TRUE) == null) {
            closeables.add(c);
        }
    }

    @Override
    public void close() throws Exception {
        Exception first = null;
        for (int i = closeables.size() - 1; i >= 0; i--) {
            try {
                closeables.get(i).close();
            } catch (Exception e) {
                if (first == null) first = e;
                else first.addSuppressed(e);
            }
        }
        closeables.clear();
        closeableSet.clear();
        if (first != null) throw first;
    }
}

