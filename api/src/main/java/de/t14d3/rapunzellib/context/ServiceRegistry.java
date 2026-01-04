package de.t14d3.rapunzellib.context;

import java.util.List;
import java.util.Optional;

public interface ServiceRegistry {
    <T> void register(Class<T> type, T instance);

    <T> Optional<T> find(Class<T> type);

    default <T> T get(Class<T> type) {
        return find(type).orElseThrow(() -> new IllegalStateException("Service not registered: " + type.getName()));
    }

    List<Class<?>> serviceTypes();

    List<Object> services();
}

