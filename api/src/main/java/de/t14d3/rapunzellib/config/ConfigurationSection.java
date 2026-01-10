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
    boolean contains(@NotNull String path);

    @NotNull Set<String> getKeys(boolean deep);

    @Nullable Object get(@NotNull String path);

    @Nullable String getString(@NotNull String path, @Nullable String def);

    default @Nullable String getString(@NotNull String path) {
        return getString(path, null);
    }

    int getInt(@NotNull String path, int def);

    default int getInt(@NotNull String path) {
        return getInt(path, 0);
    }

    boolean getBoolean(@NotNull String path, boolean def);

    default boolean getBoolean(@NotNull String path) {
        return getBoolean(path, false);
    }

    double getDouble(@NotNull String path, double def);

    default double getDouble(@NotNull String path) {
        return getDouble(path, 0D);
    }

    @NotNull List<String> getStringList(@NotNull String path, @NotNull List<String> def);

    default @NotNull List<String> getStringList(@NotNull String path) {
        return getStringList(path, List.of());
    }

    @Nullable ConfigurationSection getConfigurationSection(@NotNull String path);

    @NotNull ConfigurationSection createSection(@NotNull String path);

    void set(@NotNull String path, @Nullable Object value);

    default void remove(@NotNull String path) {
        set(path, null);
    }

    @Nullable String getComment(@NotNull String path);

    void setComment(@NotNull String path, @NotNull String comment);
}
