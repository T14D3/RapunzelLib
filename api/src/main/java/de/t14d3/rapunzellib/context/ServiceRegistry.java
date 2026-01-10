package de.t14d3.rapunzellib.context;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface ServiceRegistry {
    <T> void register(@NotNull Class<T> type, @NotNull T instance);

    <T> @NotNull Optional<T> find(@NotNull Class<T> type);

    default <T> @NotNull T get(@NotNull Class<T> type) {
        return find(type).orElseThrow(() -> new IllegalStateException("Service not registered: " + type.getName()));
    }

    @NotNull List<Class<?>> serviceTypes();

    @NotNull List<Object> services();
}

