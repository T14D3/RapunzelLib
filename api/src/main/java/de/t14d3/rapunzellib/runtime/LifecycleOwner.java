package de.t14d3.rapunzellib.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Wraps a lifecycle owner object (typically a plugin instance) for type-safe access.
 */
public final class LifecycleOwner {
    private final Object value;

    /**
     * Creates a lifecycle owner wrapping the given object.
     *
     * @param value the owner object
     */
    public LifecycleOwner(@NotNull Object value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    /**
     * Returns the raw owner object.
     *
     * @return the raw owner
     */
    public @NotNull Object raw() {
        return value;
    }

    /**
     * Checks whether the owner is an instance of the given type.
     *
     * @param type the type to check
     * @return true if the owner is of the given type
     */
    public boolean is(@NotNull Class<?> type) {
        Objects.requireNonNull(type, "type");
        return type.isInstance(value);
    }

    /**
     * Casts the owner to the given type, if applicable.
     *
     * @param type the target type
     * @param <T>  the target type
     * @return an {@link Optional} containing the cast owner, or empty if not of that type
     */
    public <T> @NotNull Optional<T> as(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (!type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    /**
     * Casts the owner to the given type, throwing if not applicable.
     *
     * @param type the target type
     * @param <T>  the target type
     * @return the cast owner
     * @throws IllegalStateException if the owner is not of the given type
     */
    public <T> @NotNull T require(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        return as(type).orElseThrow(() -> new IllegalStateException(
            "Runtime lifecycle owner is " + value.getClass().getName() + ", not " + type.getName()
        ));
    }
}
