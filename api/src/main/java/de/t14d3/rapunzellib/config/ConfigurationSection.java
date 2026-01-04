package de.t14d3.rapunzellib.config;

import java.util.List;
import java.util.Set;

/**
 * A view onto a subtree of a {@link YamlConfig}.
 *
 * <p>Paths passed to this interface are relative to the section's root.</p>
 */
public interface ConfigurationSection {
    boolean contains(String path);

    Set<String> getKeys(boolean deep);

    Object get(String path);

    String getString(String path, String def);

    default String getString(String path) {
        return getString(path, null);
    }

    int getInt(String path, int def);

    default int getInt(String path) {
        return getInt(path, 0);
    }

    boolean getBoolean(String path, boolean def);

    default boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    double getDouble(String path, double def);

    default double getDouble(String path) {
        return getDouble(path, 0D);
    }

    List<String> getStringList(String path, List<String> def);

    default List<String> getStringList(String path) {
        return getStringList(path, List.of());
    }

    ConfigurationSection getConfigurationSection(String path);

    ConfigurationSection createSection(String path);

    void set(String path, Object value);

    default void remove(String path) {
        set(path, null);
    }

    String getComment(String path);

    void setComment(String path, String comment);
}

