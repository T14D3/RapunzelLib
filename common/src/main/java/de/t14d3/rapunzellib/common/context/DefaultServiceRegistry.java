package de.t14d3.rapunzellib.common.context;

import de.t14d3.rapunzellib.context.ServiceRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultServiceRegistry implements ServiceRegistry {
    private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        services.put(type, instance);
    }

    @Override
    public <T> Optional<T> find(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object instance = services.get(type);
        if (instance == null) return Optional.empty();
        return Optional.of(type.cast(instance));
    }

    @Override
    public List<Class<?>> serviceTypes() {
        return services.keySet().stream().toList();
    }

    @Override
    public List<Object> services() {
        return services.values().stream().toList();
    }
}
