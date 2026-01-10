package de.t14d3.rapunzellib.config;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class YamlConfigSection implements ConfigurationSection {
    private final YamlConfig config;
    private final String prefix;

    YamlConfigSection(YamlConfig config, String prefix) {
        this.config = Objects.requireNonNull(config, "config");
        this.prefix = prefix == null ? "" : prefix.trim();
    }

    @Override
    public boolean contains(@NotNull String path) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return false;
        return config.contains(abs);
    }

    @Override
    public @NotNull Set<String> getKeys(boolean deep) {
        if (prefix.isEmpty()) {
            return config.keys(deep);
        }

        Object v = config.get(prefix);
        if (!(v instanceof Map<?, ?> map)) return Set.of();

        if (!deep) {
            Set<String> out = new LinkedHashSet<>();
            for (Object key : map.keySet()) {
                if (key != null) out.add(String.valueOf(key));
            }
            return out;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> section = (Map<String, Object>) map;
        Set<String> out = new LinkedHashSet<>();
        collectKeys(out, "", section);
        return out;
    }

    @Override
    public Object get(@NotNull String path) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return null;
        return config.get(abs);
    }

    @Override
    public String getString(@NotNull String path, String def) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return def;
        return config.getString(abs, def);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return def;
        return config.getInt(abs, def);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return def;
        return config.getBoolean(abs, def);
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return def;
        return config.getDouble(abs, def);
    }

    @Override
    public @NotNull List<String> getStringList(@NotNull String path, @NotNull List<String> def) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return def;
        return config.getStringList(abs, def);
    }

    @Override
    public ConfigurationSection getConfigurationSection(@NotNull String path) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return this;
        Object v = config.get(abs);
        if (v instanceof Map<?, ?>) return new YamlConfigSection(config, abs);
        return null;
    }

    @Override
    public @NotNull ConfigurationSection createSection(@NotNull String path) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return this;

        Object v = config.get(abs);
        if (!(v instanceof Map<?, ?>)) {
            config.set(abs, new LinkedHashMap<String, Object>());
        }
        return new YamlConfigSection(config, abs);
    }

    @Override
    public void set(@NotNull String path, Object value) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return;
        config.set(abs, value);
    }

    @Override
    public String getComment(@NotNull String path) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return null;
        return config.getComment(abs);
    }

    @Override
    public void setComment(@NotNull String path, @NotNull String comment) {
        String abs = abs(path);
        if (abs == null || abs.isBlank()) return;
        config.setComment(abs, comment);
    }

    private String abs(String path) {
        if (path == null) return null;
        String trimmed = path.trim();
        if (trimmed.isEmpty()) return prefix;
        if (prefix.isEmpty()) return trimmed;
        return prefix + "." + trimmed;
    }

    private static void collectKeys(Set<String> out, String relativePrefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey();
            if (key == null || key.isBlank()) continue;
            String rel = relativePrefix.isEmpty() ? key : relativePrefix + "." + key;
            out.add(rel);

            Object v = e.getValue();
            if (v instanceof Map<?, ?> child) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) child;
                collectKeys(out, rel, m);
            }
        }
    }
}

