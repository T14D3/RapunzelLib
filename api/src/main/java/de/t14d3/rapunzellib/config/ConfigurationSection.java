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
     * @param path the path to check
     * @return true if a value exists
     */
    boolean contains(@NotNull String path);

    /**
     * Returns all keys in this section.
     *
     * @param deep whether to include nested keys
     * @return a set of keys
     */
    @NotNull Set<String> getKeys(boolean deep);

    /**
     * Returns the raw value at the given path.
     *
     * @param path the path to read
     * @return the value, or null if not found
     */
    @Nullable Object get(@NotNull String path);

    /**
     * Returns a string value at the given path, with a default fallback.
     *
     * @param path the path to read
     * @param def  the default value
     * @return the string value, or def if not found
     */
    @Nullable String getString(@NotNull String path, @Nullable String def);

    /**
     * Returns a string value at the given path, or null.
     *
     * @param path the path to read
     * @return the string value, or null if not found
     */
    default @Nullable String getString(@NotNull String path) {
        return getString(path, null);
    }

    /**
     * Returns an integer value at the given path, with a default fallback.
     *
     * @param path the path to read
     * @param def  the default value
     * @return the int value, or def if not found
     */
    int getInt(@NotNull String path, int def);

    /**
     * Returns an integer value at the given path, defaulting to 0.
     *
     * @param path the path to read
     * @return the int value, or 0 if not found
     */
    default int getInt(@NotNull String path) {
        return getInt(path, 0);
    }

    /**
     * Returns a boolean value at the given path, with a default fallback.
     *
     * @param path the path to read
     * @param def  the default value
     * @return the boolean value, or def if not found
     */
    boolean getBoolean(@NotNull String path, boolean def);

    /**
     * Returns a boolean value at the given path, defaulting to false.
     *
     * @param path the path to read
     * @return the boolean value, or false if not found
     */
    default boolean getBoolean(@NotNull String path) {
        return getBoolean(path, false);
    }

    /**
     * Returns a double value at the given path, with a default fallback.
     *
     * @param path the path to read
     * @param def  the default value
     * @return the double value, or def if not found
     */
    double getDouble(@NotNull String path, double def);

    /**
     * Returns a double value at the given path, defaulting to 0.
     *
     * @param path the path to read
     * @return the double value, or 0 if not found
     */
    default double getDouble(@NotNull String path) {
        return getDouble(path, 0D);
    }

    /**
     * Returns a string list value at the given path, with a default fallback.
     *
     * @param path the path to read
     * @param def  the default value
     * @return the string list, or def if not found
     */
    @NotNull List<String> getStringList(@NotNull String path, @NotNull List<String> def);

    /**
     * Returns a string list value at the given path, defaulting to an empty list.
     *
     * @param path the path to read
     * @return the string list, or an empty list if not found
     */
    default @NotNull List<String> getStringList(@NotNull String path) {
        return getStringList(path, List.of());
    }

    /**
     * Returns a nested configuration section at the given path.
     *
     * @param path the path to the section
     * @return the configuration section, or null if not a mapping
     */
    @Nullable ConfigurationSection getConfigurationSection(@NotNull String path);

    /**
     * Creates a nested configuration section at the given path.
     *
     * @param path the path for the new section
     * @return the created section
     */
    @NotNull ConfigurationSection createSection(@NotNull String path);

    /**
     * Sets a value at the given path.
     *
     * @param path  the path to set
     * @param value the value to set, or null to remove
     */
    void set(@NotNull String path, @Nullable Object value);

    /**
     * Removes the value at the given path.
     *
     * @param path the path to remove
     */
    default void remove(@NotNull String path) {
        set(path, null);
    }

    /**
     * Returns the comment at the given path.
     *
     * @param path the path to read
     * @return the comment, or null if not set
     */
    @Nullable String getComment(@NotNull String path);

    /**
     * Sets a comment at the given path.
     *
     * @param path    the path to comment
     * @param comment the comment text
     */
    void setComment(@NotNull String path, @NotNull String comment);
}
