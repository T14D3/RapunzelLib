package de.t14d3.rapunzellib.config;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.message.MessageKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface YamlConfig {
    boolean contains(@NotNull String path);

    @NotNull Set<String> keys(boolean deep);

    @Nullable Object get(@NotNull String path);

    default <T> @Nullable T get(@NotNull String path, @NotNull Class<T> type) {
        return get(path, type, null);
    }

    default <T> @NotNull Optional<T> getOptional(@NotNull String path, @NotNull Class<T> type) {
        return Optional.ofNullable(get(path, type, null));
    }

    /**
     * Returns the value at {@code path} coerced to {@code type} (if possible), or {@code def} if missing/uncoercible.
     */
    default <T> @Nullable T get(@NotNull String path, @NotNull Class<T> type, @Nullable T def) {
        Object value = get(path);
        if (value == null) return def;
        if (type.isInstance(value)) return type.cast(value);
        return def;
    }

    @Nullable String getString(@NotNull String path, @Nullable String def);

    default @Nullable String getString(@NotNull String path) {
        return getString(path, null);
    }

    int getInt(@NotNull String path, int def);

    default int getInt(@NotNull String path) {
        return getInt(path, 0);
    }

    default long getLong(@NotNull String path, long def) {
        Long v = get(path, Long.class, null);
        if (v != null) return v;
        Integer i = get(path, Integer.class, null);
        if (i != null) return i.longValue();
        return def;
    }

    default long getLong(@NotNull String path) {
        return getLong(path, 0L);
    }

    boolean getBoolean(@NotNull String path, boolean def);

    default boolean getBoolean(@NotNull String path) {
        return getBoolean(path, false);
    }

    double getDouble(@NotNull String path, double def);

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

    void set(@NotNull String path, @Nullable Object value);

    default void remove(@NotNull String path) {
        set(path, null);
    }

    default <T> @Nullable T getOrSetDefault(@NotNull String path, @NotNull Class<T> type, @Nullable T def) {
        T existing = get(path, type, null);
        if (existing != null) return existing;
        if (def != null) set(path, def);
        return def;
    }

    @Nullable String getComment(@NotNull String path);

    void setComment(@NotNull String path, @NotNull String comment);

    void save();

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
