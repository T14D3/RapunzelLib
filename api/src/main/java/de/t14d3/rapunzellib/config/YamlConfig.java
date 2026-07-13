package de.t14d3.rapunzellib.config;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.message.MessageKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A YAML configuration file with typed read access, comments support, and save/reload.
 */
public interface YamlConfig {
    /** Checks whether a value exists at the given path. */
    boolean contains(@NotNull String path);

    /** Returns all keys in this config. */
    @NotNull Set<String> keys(boolean deep);

    /** Returns the raw value at the given path. */
    @Nullable Object get(@NotNull String path);

    /** Returns the value at the given path coerced to the specified type. */
    default <T> @Nullable T get(@NotNull String path, @NotNull Class<T> type) {
        return get(path, type, null);
    }

    /** Returns the value at the given path as an Optional. */
    default <T> @NotNull Optional<T> getOptional(@NotNull String path, @NotNull Class<T> type) {
        return Optional.ofNullable(get(path, type, null));
    }

    /**
     * Returns the value at {@code path} coerced to {@code type} (if possible), or {@code def} if missing/uncoercible.
     */
    @Contract("_, _, !null -> !null")
    default <T> @Nullable T get(@NotNull String path, @NotNull Class<T> type, @Nullable T def) {
        Object value = get(path);
        if (value == null) return def;
        if (type.isInstance(value)) return type.cast(value);
        return def;
    }

    /** Returns a string value at the given path, with a default fallback. */
    @Nullable String getString(@NotNull String path, @Nullable String def);

    /** Returns a string value at the given path, or null. */
    default @Nullable String getString(@NotNull String path) {
        return getString(path, null);
    }

    /** Returns an integer value at the given path, with a default fallback. */
    int getInt(@NotNull String path, int def);

    /** Returns an integer value at the given path, defaulting to 0. */
    default int getInt(@NotNull String path) {
        return getInt(path, 0);
    }

    /** Returns a long value at the given path, with a default fallback. */
    default long getLong(@NotNull String path, long def) {
        Long v = get(path, Long.class, null);
        if (v != null) return v;
        Integer i = get(path, Integer.class, null);
        if (i != null) return i.longValue();
        return def;
    }

    /** Returns a long value at the given path, defaulting to 0. */
    default long getLong(@NotNull String path) {
        return getLong(path, 0L);
    }

    /** Returns a boolean value at the given path, with a default fallback. */
    boolean getBoolean(@NotNull String path, boolean def);

    /** Returns a boolean value at the given path, defaulting to false. */
    default boolean getBoolean(@NotNull String path) {
        return getBoolean(path, false);
    }

    /** Returns a double value at the given path, with a default fallback. */
    double getDouble(@NotNull String path, double def);

    /** Returns a double value at the given path, defaulting to 0. */
    default double getDouble(@NotNull String path) {
        return getDouble(path, 0D);
    }

    default float getFloat(@NotNull String path, float def) {
        return (float) getDouble(path, def);
    }

    default float getFloat(@NotNull String path) {
        return getFloat(path, 0F);
    }

    default @NotNull List<?> getList(@NotNull String path, @NotNull List<?> def) {
        Object v = get(path);
        if (v instanceof List<?> list) return list;
        return def;
    }

    default @NotNull List<?> getList(@NotNull String path) {
        return getList(path, List.of());
    }

    default @NotNull List<String> getStringList(@NotNull String path, @NotNull List<String> def) {
        Object v = get(path);
        if (!(v instanceof List<?> list)) return def;
        return list.stream().map(String::valueOf).toList();
    }

    default @NotNull List<String> getStringList(@NotNull String path) {
        return getStringList(path, List.of());
    }

    default @Nullable UUID getUUID(@NotNull String path, @Nullable UUID def) {
        return get(path, UUID.class, def);
    }

    default @Nullable Duration getDuration(@NotNull String path, @Nullable Duration def) {
        return get(path, Duration.class, def);
    }

    default @Nullable PlatformId getPlatformId(@NotNull String path, @Nullable PlatformId def) {
        return get(path, PlatformId.class, def);
    }

    default @Nullable MessageKey getMessageKey(@NotNull String path, @Nullable MessageKey def) {
        return get(path, MessageKey.class, def);
    }

    default @Nullable RBlockPos getBlockPos(@NotNull String path, @Nullable RBlockPos def) {
        return get(path, RBlockPos.class, def);
    }

    default @Nullable RWorldRef getWorldRef(@NotNull String path, @Nullable RWorldRef def) {
        return get(path, RWorldRef.class, def);
    }

    default @Nullable RLocation getLocation(@NotNull String path, @Nullable RLocation def) {
        return get(path, RLocation.class, def);
    }

    default <E extends Enum<E>> @Nullable E getEnum(
        @NotNull String path,
        @NotNull Class<E> enumType,
        @Nullable E def
    ) {
        return get(path, enumType, def);
    }

    /** Sets a value at the given path. */
    void set(@NotNull String path, @Nullable Object value);

    /** Removes the value at the given path. */
    default void remove(@NotNull String path) {
        set(path, null);
    }

    /**
     * Returns the value at the given path, or sets and returns the default if absent.
     *
     * <p>If the value exists at the path, it is returned as-is. Otherwise, the
     * default value is written to the config and returned. This is useful for
     * populating config files with safe defaults on first access.</p>
     *
     * @param path the configuration path
     * @param type the expected value type
     * @param def  the default value to set and return if absent (may be null)
     * @param <T>  the value type
     * @return the existing value, or the default if no value was present
     */
    default <T> @Nullable T getOrSetDefault(@NotNull String path, @NotNull Class<T> type, @Nullable T def) {
        T existing = get(path, type, null);
        if (existing != null) return existing;
        if (def != null) set(path, def);
        return def;
    }

    /** Returns the comment at the given path. */
    @Nullable String getComment(@NotNull String path);

    /** Sets a comment at the given path. */
    void setComment(@NotNull String path, @NotNull String comment);

    /**
     * Saves the config to disk.
     *
     * <p>Writes the current in-memory state to the file path this config was loaded from.
     * Preserves comments if supported by the implementation.</p>
     */
    void save();

    /**
     * Reloads the config from disk.
     *
     * <p>Discards any in-memory changes and re-reads the file from disk.</p>
     */
    void reload();

    /** Returns a nested section at {@code path}, or {@code null} if the value at that path is not a mapping. */
    default @Nullable ConfigurationSection getConfigurationSection(@NotNull String path) {
        if (path.isBlank()) return new YamlConfigSection(this, "");
        Object v = get(path);
        if (v instanceof Map<?, ?>) return new YamlConfigSection(this, path);
        return null;
    }

    /** Ensures a mapping exists at {@code path} and returns it as a section. */
    default @NotNull ConfigurationSection createSection(@NotNull String path) {
        if (path.isBlank()) return new YamlConfigSection(this, "");
        Object v = get(path);
        if (!(v instanceof Map<?, ?>)) {
            set(path, new LinkedHashMap<String, Object>());
        }
        return new YamlConfigSection(this, path);
    }
}
