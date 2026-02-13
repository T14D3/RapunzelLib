package de.t14d3.rapunzellib.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class LifecycleOwner {
    private final Object value;

    public LifecycleOwner(@NotNull Object value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public @NotNull Object raw() {
        return value;
    }

    public boolean is(@NotNull Class<?> type) {
        Objects.requireNonNull(type, "type");
        return type.isInstance(value);
    }

    public <T> @NotNull Optional<T> as(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (!type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    public <T> @NotNull T require(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        return as(type).orElseThrow(() -> new IllegalStateException(
            "Runtime lifecycle owner is " + value.getClass().getName() + ", not " + type.getName()
        ));
    }
}
