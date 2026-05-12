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

/**
 * Default implementation of {@link RapunzelContext}.
 * <p>
 * Wires together the platform runtime, logging, data directory, resource provider,
 * and a contextual scheduler. Manages a list of closeable resources that are
 * cleaned up in reverse registration order when the context is closed.
 */
public final class DefaultRapunzelContext implements RapunzelContext {
    /** Shared runtime instance across all contexts */
    private final RapunzelRuntime sharedRuntime;
    /** Platform runtime descriptor */
    private final PlatformRuntime runtime;
    /** Logger for this context */
    private final Logger logger;
    /** Plugin data directory */
    private final Path dataDirectory;
    /** Resource provider for classpath resources */
    private final ResourceProvider resources;
    /** Context-bound scheduler */
    private final Scheduler scheduler;
    /** Service registry */
    private final DefaultServiceRegistry services = new DefaultServiceRegistry();

    /** Ordered list of closeable resources (LIFO order on close) */
    private final List<AutoCloseable> closeables = new ArrayList<>();
    /** Identity set to prevent duplicate registration of closeables */
    private final IdentityHashMap<AutoCloseable, Boolean> closeableSet = new IdentityHashMap<>();

    /**
     * Creates a context using the global {@link RapunzelRuntime#getInstance()}.
     *
     * @param runtime       the platform runtime descriptor
     * @param logger        the logger
     * @param dataDirectory the plugin data directory
     * @param resources     the resource provider
     * @param scheduler     the platform scheduler
     */
    public DefaultRapunzelContext(
        PlatformRuntime runtime,
        Logger logger,
        Path dataDirectory,
        ResourceProvider resources,
        Scheduler scheduler
    ) {
        this(RapunzelRuntime.getInstance(), runtime, logger, dataDirectory, resources, scheduler);
    }

    /**
     * Creates a context with an explicit shared runtime.
     *
     * @param sharedRuntime the shared runtime instance
     * @param runtime       the platform runtime descriptor
     * @param logger        the logger
     * @param dataDirectory the plugin data directory
     * @param resources     the resource provider
     * @param scheduler     the platform scheduler
     */
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

    /**
     * Gets the shared runtime instance.
     *
     * @return the shared runtime
     */
    @Override
    public @NotNull RapunzelRuntime sharedRuntime() {
        return sharedRuntime;
    }

    /**
     * Gets the platform runtime descriptor.
     *
     * @return the platform runtime
     */
    @Override
    public @NotNull PlatformRuntime runtime() {
        return runtime;
    }

    /**
     * Gets the logger.
     *
     * @return the logger
     */
    @Override
    public @NotNull Logger logger() {
        return logger;
    }

    /**
     * Gets the plugin data directory.
     *
     * @return the data directory path
     */
    @Override
    public @NotNull Path dataDirectory() {
        return dataDirectory;
    }

    /**
     * Gets the resource provider.
     *
     * @return the resource provider
     */
    @Override
    public @NotNull ResourceProvider resources() {
        return resources;
    }

    /**
     * Gets the context-bound scheduler.
     *
     * @return the scheduler
     */
    @Override
    public @NotNull Scheduler scheduler() {
        return scheduler;
    }

    /**
     * Gets the service registry.
     *
     * @return the service registry
     */
    @Override
    public @NotNull ServiceRegistry services() {
        return services;
    }

    /**
     * Registers a service instance, also tracking it as a closeable if it implements
     * {@link AutoCloseable}.
     *
     * @param type     the service type class
     * @param instance the service instance
     * @param <T>      the service type
     * @return the registered instance
     */
    @Override
    public <T> @NotNull T register(@NotNull Class<T> type, @NotNull T instance) {
        services.register(type, instance);
        if (instance instanceof AutoCloseable closeable) {
            registerCloseable(closeable);
        }
        return instance;
    }

    /**
     * Registers an {@link AutoCloseable} to be closed when the context shuts down.
     *
     * @param closeable the closeable resource
     */
    @Override
    public void registerCloseable(@NotNull AutoCloseable closeable) {
        AutoCloseable c = Objects.requireNonNull(closeable, "closeable");
        if (closeableSet.putIfAbsent(c, Boolean.TRUE) == null) {
            closeables.add(c);
        }
    }

    /**
     * Closes all registered closeables in reverse registration order.
     * Exceptions from individual closeables are collected and the first is re-thrown
     * with subsequent exceptions suppressed.
     *
     * @throws Exception if any closeable throws during shutdown
     */
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
