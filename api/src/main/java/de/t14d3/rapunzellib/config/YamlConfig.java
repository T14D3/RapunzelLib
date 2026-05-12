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
    /**
     * Checks whether a value exists at the given path.
     *
     * @param path the path to check
     * @return true if a value exists
     */
    boolean contains(@NotNull String path);

    /**
     * Returns all keys in this config.
     *
     * @param deep whether to include nested keys
     * @return a set of keys
     */
    @NotNull Set<String> keys(boolean deep);

    /**
     * Returns the raw value at the given path.
     *
     * @param path the path to read
     * @return the value, or null if not found
     */
    @Nullable Object get(@NotNull String path);

    /**
     * Returns the value at the given path coerced to the specified type.
     *
     * @param path the path to read
     * @param type the expected type
     * @param <T>  the type
     * @return the value, or null if missing or uncoercible
     */
    default <T> @Nullable T get(@NotNull String path, @NotNull Class<T> type) {
        return get(path, type, null);
    }

    /**
     * Returns the value at the given path as an Optional.
     *
     * @param path the path to read
     * @param type the expected type
     * @param <T>  the type
     * @return an Optional containing the value, or empty if missing
     */
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
     * Returns a long value at the given path, with a default fallback.
     *
     * @param path the path to read
     * @param def  the default value
     * @return the long value, or def if not found
     */
    default long getLong(@NotNull String path, long def) {
        Long v = get(path, Long.class, null);
        if (v != null) return v;
        Integer i = get(path, Integer.class, null);
        if (i != null) return i.longValue();
        return def;
    }

    /**
     * Returns a long value at the given path, defaulting to 0.
     *
     * @param path the path to read
     * @return the long value, or 0 if not found
     */
    default long getLong(@NotNull String path) {
        return getLong(path, 0L);
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

    default <T> @Nullable T getOrSetDefault(@NotNull String path, @NotNull Class<T> type, @Nullable T def) {
        T existing = get(path, type, null);
        if (existing != null) return existing;
        if (def != null) set(path, def);
        return def;
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

    /**
     * Saves the config to disk.
     */
    void save();

    /**
     * Reloads the config from disk.
     */
    void reload();

    /**
     * Returns a nested section at {@code path}, or {@code null} if the value at that path is not a mapping.
     */
    default @Nullable ConfigurationSection getConfigurationSection(@NotNull String path) {
        if (path.isBlank()) return new YamlConfigSection(this, "");
        Object v = get(path);
        if (v instanceof Map<?, ?>) return new YamlConfigSection(this, path);
        return null;
    }

    /**
     * Ensures a mapping exists at {@code path} and returns it as a section.
     */
    default @NotNull ConfigurationSection createSection(@NotNull String path) {
        if (path.isBlank()) return new YamlConfigSection(this, "");
        Object v = get(path);
        if (!(v instanceof Map<?, ?>)) {
            set(path, new LinkedHashMap<String, Object>());
        }
        return new YamlConfigSection(this, path);
    }
}
