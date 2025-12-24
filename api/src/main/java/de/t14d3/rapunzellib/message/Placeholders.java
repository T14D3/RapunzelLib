package de.t14d3.rapunzellib.message;

import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simplify Placeholder/Replacements in a Component.
 */
public final class Placeholders {
    private static final Placeholders EMPTY = new Placeholders(Map.of(), Map.of());

    public static Placeholders empty() {
        return EMPTY;
    }

    /**
     * Creates a new Placeholders Builder.
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private final Map<String, String> strings;
    private final Map<String, Component> components;

    private Placeholders(Map<String, String> strings, Map<String, Component> components) {
        this.strings = strings;
        this.components = components;
    }

    public Map<String, String> strings() {
        return strings;
    }

    public Map<String, Component> components() {
        return components;
    }

    public static final class Builder {
        private final Map<String, String> strings = new LinkedHashMap<>();
        private final Map<String, Component> components = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder string(String name, String value) {
            strings.put(requireName(name), Objects.toString(value, ""));
            return this;
        }

        public Builder component(String name, Component value) {
            components.put(requireName(name), Objects.requireNonNull(value, "value"));
            return this;
        }

        public Placeholders build() {
            if (strings.isEmpty() && components.isEmpty()) return EMPTY;
            return new Placeholders(
                Collections.unmodifiableMap(new LinkedHashMap<>(strings)),
                Collections.unmodifiableMap(new LinkedHashMap<>(components))
            );
        }

        private static String requireName(String name) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("name cannot be null/blank");
            return name;
        }
    }
}
