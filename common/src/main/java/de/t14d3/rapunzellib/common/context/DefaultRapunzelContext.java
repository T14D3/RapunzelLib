package de.t14d3.rapunzellib.common.context;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
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
    public PlatformId platformId() {
        return platformId;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public ResourceProvider resources() {
        return resources;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public ServiceRegistry services() {
        return services;
    }

    public <T> T register(Class<T> type, T instance) {
        services.register(type, instance);
        if (instance instanceof AutoCloseable closeable) {
            closeables.add(closeable);
        }
        return instance;
    }

    public void registerCloseable(AutoCloseable closeable) {
        closeables.add(Objects.requireNonNull(closeable, "closeable"));
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
        if (first != null) throw first;
    }
}

