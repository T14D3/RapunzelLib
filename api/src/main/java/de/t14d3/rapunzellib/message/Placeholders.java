package de.t14d3.rapunzellib.message;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simplify Placeholder/Replacements in a Component.
 */
public final class Placeholders {
    private static final Placeholders EMPTY = new Placeholders(Map.of(), Map.of());

    public static @NotNull Placeholders empty() {
        return EMPTY;
    }

    /**
     * Creates a new Placeholders Builder.
     * @return The builder.
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    private final Map<String, String> strings;
    private final Map<String, Component> components;

    private Placeholders(Map<String, String> strings, Map<String, Component> components) {
        this.strings = strings;
        this.components = components;
    }

    /**
     * Returns the string placeholders map.
     *
     * @return the string placeholders
     */
    public @NotNull Map<String, String> strings() {
        return strings;
    }

    /**
     * Returns the component placeholders map.
     *
     * @return the component placeholders
     */
    public @NotNull Map<String, Component> components() {
        return components;
    }

    public static final class Builder {
        private final Map<String, String> strings = new LinkedHashMap<>();
        private final Map<String, Component> components = new LinkedHashMap<>();

        private Builder() {
        }

    /**
     * Adds a string placeholder value.
     *
     * @param name  the placeholder name
     * @param value the string value, null treated as empty
     * @return this builder
     */
    public @NotNull Builder string(@NotNull String name, @Nullable String value) {
            strings.put(requireName(name), Objects.toString(value, ""));
            return this;
        }

        /**
         * Adds a component placeholder value.
         *
         * @param name  the placeholder name
         * @param value the component value
         * @return this builder
         */
        public @NotNull Builder component(@NotNull String name, @NotNull Component value) {
            components.put(requireName(name), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Builds the {@link Placeholders} instance.
         *
         * @return the built placeholders
         */
        public @NotNull Placeholders build() {
            if (strings.isEmpty() && components.isEmpty()) return EMPTY;
            return new Placeholders(
                Collections.unmodifiableMap(new LinkedHashMap<>(strings)),
                Collections.unmodifiableMap(new LinkedHashMap<>(components))
            );
        }

        private static @NotNull String requireName(@NotNull String name) {
            if (name.isBlank()) throw new IllegalArgumentException("name cannot be null/blank");
            return name;
        }
    }
}
