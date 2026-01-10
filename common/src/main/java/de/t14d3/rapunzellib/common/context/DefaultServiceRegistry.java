package de.t14d3.rapunzellib.common.context;

import de.t14d3.rapunzellib.context.ServiceRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultServiceRegistry implements ServiceRegistry {
    private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        services.put(type, instance);
    }

    @Override
    public <T> @NotNull Optional<T> find(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object instance = services.get(type);
        if (instance == null) return Optional.empty();
        return Optional.of(type.cast(instance));
    }

    @Override
    public @NotNull List<Class<?>> serviceTypes() {
        return services.keySet().stream().toList();
    }

    @Override
    public @NotNull List<Object> services() {
        return services.values().stream().toList();
    }
}
