package de.t14d3.rapunzellib.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shared RapunzelLib runtime providing global service registration and lifecycle management.
 *
 * <p>This is a singleton accessed via {@link #getInstance()} and serves as the
 * top-level service container for cross-context services.</p>
 */
public final class RapunzelRuntime {
    private static final RapunzelRuntime INSTANCE = new RapunzelRuntime();

    private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    private RapunzelRuntime() {
    }

    /**
     * Returns the singleton instance.
     *
     * @return the runtime instance
     */
    public static @NotNull RapunzelRuntime getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a runtime service.
     *
     * @param type     the service type
     * @param instance the service instance
     * @param <T>      the service type
     */
    public <T> void register(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        services.put(type, type.cast(instance));
    }

    /**
     * Registers a runtime service if no instance is already registered.
     *
     * @param type     the service type
     * @param instance the service instance
     * @param <T>      the service type
     * @return the registered or existing instance
     */
    public <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        return type.cast(services.putIfAbsent(type, type.cast(instance)) == null ? instance : services.get(type));
    }

    /**
     * Gets an existing runtime service or creates and registers one from the supplier.
     *
     * @param type     the service type
     * @param supplier the supplier to create the instance
     * @param <T>      the service type
     * @return the existing or newly created instance
     */
    public <T> @NotNull T getOrCreate(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(supplier, "supplier");
        return type.cast(services.computeIfAbsent(type, ignored -> type.cast(Objects.requireNonNull(
            supplier.get(),
            "supplier returned null for " + type.getName()
        ))));
    }

    /**
     * Finds a runtime service by type.
     *
     * @param type the service type
     * @param <T>  the service type
     * @return an {@link Optional} containing the instance, or empty if not registered
     */
    public <T> @NotNull Optional<T> find(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        return Optional.ofNullable(services.get(type)).map(type::cast);
    }

    /**
     * Requires a runtime service, throwing if not registered.
     *
     * @param type the service type
     * @param <T>  the service type
     * @return the service instance
     * @throws IllegalStateException if the service is not registered
     */
    public <T> @NotNull T get(@NotNull Class<T> type) {
        return find(type).orElseThrow(() -> new IllegalStateException("Runtime service not registered: " + type.getName()));
    }

    /**
     * Returns all registered runtime service types.
     *
     * @return a list of service types
     */
    public @NotNull List<Class<?>> serviceTypes() {
        return List.copyOf(services.keySet());
    }

    /**
     * Shuts down all registered runtime services, closing any {@link AutoCloseable} instances.
     *
     * @throws Exception if any service fails to close
     */
    public void shutdown() throws Exception {
        Object[] snapshot = services.values().toArray();
        services.clear();

        Exception first = null;
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        for (Object service : snapshot) {
            if (seen.put(service, Boolean.TRUE) != null || !(service instanceof AutoCloseable closeable)) {
                continue;
            }
            try {
                closeable.close();
            } catch (Exception e) {
                if (first == null) first = e;
                else first.addSuppressed(e);
            }
        }
        if (first != null) {
            throw first;
        }
    }
}
