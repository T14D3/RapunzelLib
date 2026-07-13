package de.t14d3.rapunzellib.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * A view onto a subtree of a {@link YamlConfig}.
 *
 * <p>Paths passed to this interface are relative to the section's root.</p>
 */
public interface ConfigurationSection {
    /**
     * Checks whether a value exists at the given path.
     *
     * @param path the configuration path (dot-separated, e.g. "database.host")
     * @return true if a value exists at the path
     */
    boolean contains(@NotNull String path);

    /** Returns all keys in this section. */
    @NotNull Set<String> getKeys(boolean deep);

    /**
     * Returns the raw value at the given path.
     *
     * @param path the configuration path
     * @return the raw value, or null if not found
     */
    @Nullable Object get(@NotNull String path);

    /**
     * Returns a string value at the given path, with a default fallback.
     *
     * @param path the configuration path
     * @param def  the default value if the path is missing or not a string
     * @return the string value, or the default
     */
    @Nullable String getString(@NotNull String path, @Nullable String def);

    /**
     * Returns a string value at the given path, or null.
     *
     * @param path the configuration path
     * @return the string value, or null if missing
     */
    default @Nullable String getString(@NotNull String path) {
        return getString(path, null);
    }

    /**
     * Returns an integer value at the given path, with a default fallback.
     *
     * @param path the configuration path
     * @param def  the default value if missing or not an integer
     * @return the integer value
     */
    int getInt(@NotNull String path, int def);

    /**
     * Returns an integer value at the given path, defaulting to 0.
     *
     * @param path the configuration path
     * @return the integer value, or 0
     */
    default int getInt(@NotNull String path) {
        return getInt(path, 0);
    }

    /**
     * Returns a boolean value at the given path, with a default fallback.
     *
     * @param path the configuration path
     * @param def  the default value if missing
     * @return the boolean value
     */
    boolean getBoolean(@NotNull String path, boolean def);

    /**
     * Returns a boolean value at the given path, defaulting to false.
     *
     * @param path the configuration path
     * @return the boolean value, or false
     */
    default boolean getBoolean(@NotNull String path) {
        return getBoolean(path, false);
    }

    /**
     * Returns a double value at the given path, with a default fallback.
     *
     * @param path the configuration path
     * @param def  the default value if missing
     * @return the double value
     */
    double getDouble(@NotNull String path, double def);

    /**
     * Returns a double value at the given path, defaulting to 0.
     *
     * @param path the configuration path
     * @return the double value, or 0
     */
    default double getDouble(@NotNull String path) {
        return getDouble(path, 0D);
    }

    /**
     * Returns a string list value at the given path, with a default fallback.
     *
     * @param path the configuration path
     * @param def  the default list if missing
     * @return the string list
     */
    @NotNull List<String> getStringList(@NotNull String path, @NotNull List<String> def);

    /**
     * Returns a string list value at the given path, defaulting to an empty list.
     *
     * @param path the configuration path
     * @return the string list, or an empty list
     */
    default @NotNull List<String> getStringList(@NotNull String path) {
        return getStringList(path, List.of());
    }

    /**
     * Returns a nested configuration section at the given path.
     *
     * @param path the path to the nested section
     * @return the nested section, or null if the value at the path is not a mapping
     */
    @Nullable ConfigurationSection getConfigurationSection(@NotNull String path);

    /**
     * Creates a nested configuration section at the given path.
     *
     * <p>If a value already exists at the path and is a mapping, returns it as a section.
     * Otherwise, replaces the value with an empty mapping and returns a section for it.</p>
     *
     * @param path the path at which to create the section
     * @return the new or existing configuration section
     */
    @NotNull ConfigurationSection createSection(@NotNull String path);

    /**
     * Sets a value at the given path.
     *
     * @param path  the configuration path
     * @param value the value to set, or null to remove
     */
    void set(@NotNull String path, @Nullable Object value);

    /** Removes the value at the given path. */
    default void remove(@NotNull String path) {
        set(path, null);
    }

    /** Returns the comment at the given path. */
    @Nullable String getComment(@NotNull String path);

    /** Sets a comment at the given path. */
    void setComment(@NotNull String path, @NotNull String comment);
}
