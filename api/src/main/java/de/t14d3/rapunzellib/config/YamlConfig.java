package de.t14d3.rapunzellib.config;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.message.MessageKey;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface YamlConfig {
    boolean contains(String path);

    Set<String> keys(boolean deep);

    Object get(String path);

    default <T> T get(String path, Class<T> type) {
        return get(path, type, null);
    }

    default <T> Optional<T> getOptional(String path, Class<T> type) {
        return Optional.ofNullable(get(path, type, null));
    }

    /**
     * Returns the value at {@code path} coerced to {@code type} (if possible), or {@code def} if missing/uncoercible.
     */
    default <T> T get(String path, Class<T> type, T def) {
        Object value = get(path);
        if (value == null) return def;
        if (type.isInstance(value)) return type.cast(value);
        return def;
    }

    String getString(String path, String def);

    default String getString(String path) {
        return getString(path, null);
    }

    int getInt(String path, int def);

    default int getInt(String path) {
        return getInt(path, 0);
    }

    default long getLong(String path, long def) {
        Long v = get(path, Long.class, null);
        if (v != null) return v;
        Integer i = get(path, Integer.class, null);
        if (i != null) return i.longValue();
        return def;
    }

    default long getLong(String path) {
        return getLong(path, 0L);
    }

    boolean getBoolean(String path, boolean def);

    default boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    double getDouble(String path, double def);

    default double getDouble(String path) {
        return getDouble(path, 0D);
    }

    default float getFloat(String path, float def) {
        return (float) getDouble(path, def);
    }

    default float getFloat(String path) {
        return getFloat(path, 0F);
    }

    default List<?> getList(String path, List<?> def) {
        Object v = get(path);
        if (v instanceof List<?> list) return list;
        return def;
    }

    default List<?> getList(String path) {
        return getList(path, List.of());
    }

    default List<String> getStringList(String path, List<String> def) {
        Object v = get(path);
        if (!(v instanceof List<?> list)) return def;
        return list.stream().map(String::valueOf).toList();
    }

    default List<String> getStringList(String path) {
        return getStringList(path, List.of());
    }

    default UUID getUUID(String path, UUID def) {
        return get(path, UUID.class, def);
    }

    default Duration getDuration(String path, Duration def) {
        return get(path, Duration.class, def);
    }

    default PlatformId getPlatformId(String path, PlatformId def) {
        return get(path, PlatformId.class, def);
    }

    default MessageKey getMessageKey(String path, MessageKey def) {
        return get(path, MessageKey.class, def);
    }

    default RBlockPos getBlockPos(String path, RBlockPos def) {
        return get(path, RBlockPos.class, def);
    }

    default RWorldRef getWorldRef(String path, RWorldRef def) {
        return get(path, RWorldRef.class, def);
    }

    default RLocation getLocation(String path, RLocation def) {
        return get(path, RLocation.class, def);
    }

    default <E extends Enum<E>> E getEnum(String path, Class<E> enumType, E def) {
        return get(path, enumType, def);
    }

    void set(String path, Object value);

    default void remove(String path) {
        set(path, null);
    }

    default <T> T getOrSetDefault(String path, Class<T> type, T def) {
        T existing = get(path, type, null);
        if (existing != null) return existing;
        if (def != null) set(path, def);
        return def;
    }

    String getComment(String path);

    void setComment(String path, String comment);

    void save();

    void reload();
}
