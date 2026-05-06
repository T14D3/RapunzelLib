package de.t14d3.rapunzellib.common.context;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.common.scheduler.ContextualScheduler;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RapunzelRuntime;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

public final class DefaultRapunzelContext implements RapunzelContext {
    private final RapunzelRuntime sharedRuntime;
    private final PlatformRuntime runtime;
    private final Logger logger;
    private final Path dataDirectory;
    private final ResourceProvider resources;
    private final Scheduler scheduler;
    private final DefaultServiceRegistry services = new DefaultServiceRegistry();

    private final List<AutoCloseable> closeables = new ArrayList<>();
    private final IdentityHashMap<AutoCloseable, Boolean> closeableSet = new IdentityHashMap<>();

    public DefaultRapunzelContext(
        PlatformRuntime runtime,
        Logger logger,
        Path dataDirectory,
        ResourceProvider resources,
        Scheduler scheduler
    ) {
        this(RapunzelRuntime.getInstance(), runtime, logger, dataDirectory, resources, scheduler);
    }

    public DefaultRapunzelContext(
        RapunzelRuntime sharedRuntime,
        PlatformRuntime runtime,
        Logger logger,
        Path dataDirectory,
        ResourceProvider resources,
        Scheduler scheduler
    ) {
        this.sharedRuntime = Objects.requireNonNull(sharedRuntime, "sharedRuntime");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.scheduler = new ContextualScheduler(this, Objects.requireNonNull(scheduler, "scheduler"));
        registerCloseable((AutoCloseable) this.scheduler);
    }

    @Override
    public @NotNull RapunzelRuntime sharedRuntime() {
        return sharedRuntime;
    }

    @Override
    public @NotNull PlatformRuntime runtime() {
        return runtime;
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
