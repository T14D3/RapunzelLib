package de.t14d3.rapunzellib.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class RapunzelRuntime {
    private static final RapunzelRuntime INSTANCE = new RapunzelRuntime();

    private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    private RapunzelRuntime() {
    }

    public static @NotNull RapunzelRuntime getInstance() {
        return INSTANCE;
    }

    public <T> void register(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        services.put(type, type.cast(instance));
    }

    public <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        return type.cast(services.putIfAbsent(type, type.cast(instance)) == null ? instance : services.get(type));
    }

    public <T> @NotNull T getOrCreate(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(supplier, "supplier");
        return type.cast(services.computeIfAbsent(type, ignored -> type.cast(Objects.requireNonNull(
            supplier.get(),
            "supplier returned null for " + type.getName()
        ))));
    }

    public <T> @NotNull Optional<T> find(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        return Optional.ofNullable(services.get(type)).map(type::cast);
    }

    public <T> @NotNull T get(@NotNull Class<T> type) {
        return find(type).orElseThrow(() -> new IllegalStateException("Runtime service not registered: " + type.getName()));
    }

    public @NotNull List<Class<?>> serviceTypes() {
        return List.copyOf(services.keySet());
    }

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
