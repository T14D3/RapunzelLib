package de.t14d3.rapunzellib.config;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.message.MessageKey;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class SnakeYamlConfig implements YamlConfig {
    private static final int INDENT = 2;

    private final Path file;
    private final ResourceProvider resources;
    private final Logger logger;
    private final String defaultResourcePath;

    private final Yaml yaml;

    private Map<String, Object> root = new LinkedHashMap<>();
    private Map<String, String> comments = new LinkedHashMap<>();

    SnakeYamlConfig(Path file, ResourceProvider resources, Logger logger, String defaultResourcePath) {
        this.file = Objects.requireNonNull(file, "file");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.defaultResourcePath = (defaultResourcePath == null || defaultResourcePath.isBlank())
            ? null
            : normalizeResourcePath(defaultResourcePath);

        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndent(INDENT);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setSplitLines(false);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        SafeConstructor constructor = new SafeConstructor(loaderOptions);
        Representer representer = new Representer(dumperOptions);
        this.yaml = new Yaml(constructor, representer, dumperOptions, loaderOptions);
    }

    @Override
    public boolean contains(@NotNull String path) {
        return resolve(path, false) != null;
    }

    @Override
    public @NotNull Set<String> keys(boolean deep) {
        if (!deep) {
            return new LinkedHashSet<>(root.keySet());
        }

        Set<String> out = new LinkedHashSet<>();
        collectKeys(out, "", root);
        return out;
    }

    @Override
    public Object get(@NotNull String path) {
        Resolved resolved = resolve(path, false);
        if (resolved == null) return null;
        return resolved.parent.get(resolved.key);
    }

    @Override
    public <T> T get(@NotNull String path, @NotNull Class<T> type, T def) {
        Object value = get(path);
        T coerced = coerce(value, type);
        return coerced != null ? coerced : def;
    }

    @Override
    public String getString(@NotNull String path, String def) {
        String v = get(path, String.class, null);
        return v != null ? v : def;
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        Integer v = get(path, Integer.class, null);
        return v != null ? v : def;
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        Boolean v = get(path, Boolean.class, null);
        return v != null ? v : def;
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        Double v = get(path, Double.class, null);
        return v != null ? v : def;
    }

    @Override
    public @NotNull List<?> getList(@NotNull String path, @NotNull List<?> def) {
        Object v = get(path);
        if (v instanceof List<?> list) return list;
        return def;
    }

    @Override
    public @NotNull List<String> getStringList(@NotNull String path, @NotNull List<String> def) {
        Object v = get(path);
        if (!(v instanceof List<?> list)) return def;
        return list.stream().map(String::valueOf).toList();
    }

    @Override
    public void set(@NotNull String path, Object value) {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) return;

        Resolved resolved = resolve(path, true);
        if (resolved == null) return;

        if (value == null) {
            resolved.parent.remove(resolved.key);
            comments.remove(path);
            return;
        }

        resolved.parent.put(resolved.key, toYamlValue(value));
    }

    @Override
    public String getComment(@NotNull String path) {
        return comments.get(path);
    }

    @Override
    public void setComment(@NotNull String path, @NotNull String comment) {
        Objects.requireNonNull(path, "path");
        if (comment.isBlank()) {
            comments.remove(path);
        } else {
            comments.put(path, comment.strip());
        }
    }

    @Override
    public void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException e) {
            logger.error("Failed to create config directory", e);
        }

        String dumped = yaml.dump(root);
        String withComments = insertComments(dumped, comments);
        try {
            Files.writeString(file, withComments, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to save YAML config {}", file, e);
        }
    }

    @Override
    public void reload() {
        try {
            if (!Files.exists(file)) {
                if (defaultResourcePath != null) {
                    copyDefaultToDisk();
                } else {
                    Files.createFile(file);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to ensure YAML config exists {}", file, e);
        }

        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read YAML config {}", file, e);
            content = "";
        }

        Map<String, Object> loaded = loadRootMap(content);
        Map<String, String> loadedComments = parseComments(content);

        this.root = loaded;
        this.comments = loadedComments;

        if (defaultResourcePath != null) {
            mergeDefaultsFromResource(defaultResourcePath);
        }
    }

    private void mergeDefaultsFromResource(String resourcePath) {
        try (InputStream in = resources.open(resourcePath).orElse(null)) {
            if (in == null) return;
            String defaultsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> defaults = loadRootMap(defaultsContent);
            Map<String, String> defaultComments = parseComments(defaultsContent);

            for (String key : flattenKeys(defaults)) {
                if (!contains(key)) {
                    Object v = getAt(defaults, key);
                    set(key, deepCopy(v));
                }

                if (getComment(key) == null) {
                    String comment = defaultComments.get(key);
                    if (comment != null) setComment(key, comment);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to merge default YAML resource {}", resourcePath, e);
        }
    }

    private void copyDefaultToDisk() {
        try (InputStream in = resources.open(defaultResourcePath).orElse(null)) {
            if (in == null) {
                Files.createFile(file);
                return;
            }
            Files.copy(in, file);
        } catch (IOException e) {
            logger.warn("Failed to copy default YAML resource {} to {}", defaultResourcePath, file, e);
            try {
                if (!Files.exists(file)) Files.createFile(file);
            } catch (IOException ignored) {
            }
        }
    }

    private Map<String, Object> loadRootMap(String content) {
        Object loaded;
        try {
            loaded = yaml.load(content);
        } catch (Exception e) {
            logger.warn("Failed to parse YAML {}", file, e);
            loaded = null;
        }

        if (loaded == null) return new LinkedHashMap<>();
        if (loaded instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) continue;
                out.put(String.valueOf(e.getKey()), sanitizeLoadedValue(e.getValue()));
            }
            return out;
        }

        logger.warn("YAML {} root is not a mapping (was {}). Treating as empty.", file, loaded.getClass().getSimpleName());
        return new LinkedHashMap<>();
    }

    private Object sanitizeLoadedValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) continue;
                out.put(String.valueOf(e.getKey()), sanitizeLoadedValue(e.getValue()));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object o : list) out.add(sanitizeLoadedValue(o));
            return out;
        }
        return value;
    }

    private static void collectKeys(Set<String> out, String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            out.add(path);

            Object v = e.getValue();
            if (v instanceof Map<?, ?> child) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) child;
                collectKeys(out, path, m);
            }
        }
    }

    private Resolved resolve(String path, boolean create) {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) return null;

        String[] parts = path.split("\\.");
        Map<String, Object> current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part.isEmpty()) return null;

            Object child = current.get(part);
            if (child instanceof Map<?, ?> mapChild) {
                @SuppressWarnings("unchecked")
                Map<String, Object> next = (Map<String, Object>) mapChild;
                current = next;
                continue;
            }

            if (!create) return null;

            LinkedHashMap<String, Object> next = new LinkedHashMap<>();
            current.put(part, next);
            current = next;
        }

        String key = parts[parts.length - 1];
        if (key.isEmpty()) return null;
        if (!create && !current.containsKey(key)) return null;

        return new Resolved(current, key);
    }

    private record Resolved(Map<String, Object> parent, String key) {
    }

    private Object toYamlValue(Object value) {
        if (value == null) return null;


        switch (value) {
            case String s -> {
                return s;
            }
            case Number n -> {
                return n;
            }
            case Boolean b -> {
                return b;
            }
            case UUID uuid -> {
                return uuid.toString();
            }
            case Duration duration -> {
                return formatDuration(duration);
            }
            case PlatformId platformId -> {
                return platformId.name();
            }
            case MessageKey(String key) -> {
                return key;
            }
            case RBlockPos(int x, int y, int z) -> {
                LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                out.put("x", x);
                out.put("y", y);
                out.put("z", z);
                return out;
            }
            case RWorldRef(String name, String key) -> {
                LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                if (name != null && !name.isBlank()) out.put("name", name);
                if (key != null && !key.isBlank()) out.put("key", key);
                return out;
            }
            case RLocation(RWorldRef world, double x, double y, double z, float yaw, float pitch) -> {
                LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                out.put("world", toYamlValue(world));
                out.put("x", x);
                out.put("y", y);
                out.put("z", z);
                if (yaw != 0F) out.put("yaw", yaw);
                if (pitch != 0F) out.put("pitch", pitch);
                return out;
            }
            case Enum<?> e -> {
                return e.name();
            }
            case Map<?, ?> map -> {
                LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() == null) continue;
                    out.put(String.valueOf(entry.getKey()), toYamlValue(entry.getValue()));
                }
                return out;
            }
            case List<?> list -> {
                List<Object> out = new ArrayList<>(list.size());
                for (Object o : list) out.add(toYamlValue(o));
                return out;
            }
            default -> {
            }
        }

        if (value.getClass().isRecord()) {
            return recordToMap(value);
        }

        return String.valueOf(value);
    }

    private LinkedHashMap<String, Object> recordToMap(Object record) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (var component : record.getClass().getRecordComponents()) {
            try {
                Object v = component.getAccessor().invoke(record);
                out.put(component.getName(), toYamlValue(v));
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private <T> T coerce(Object value, Class<T> type) {
        if (value == null) return null;
        if (type.isInstance(value)) return type.cast(value);

        if (type == String.class) return (T) String.valueOf(value);

        if (type == Integer.class) {
            if (value instanceof Number n) return (T) Integer.valueOf(n.intValue());
            if (value instanceof String s) {
                try {
                    return (T) Integer.valueOf(Integer.parseInt(s.trim()));
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        if (type == Long.class) {
            if (value instanceof Number n) return (T) Long.valueOf(n.longValue());
            if (value instanceof String s) {
                try {
                    return (T) Long.valueOf(Long.parseLong(s.trim()));
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        if (type == Double.class) {
            if (value instanceof Number n) return (T) Double.valueOf(n.doubleValue());
            if (value instanceof String s) {
                try {
                    return (T) Double.valueOf(Double.parseDouble(s.trim()));
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        if (type == Boolean.class) {
            if (value instanceof Boolean b) return (T) b;
            if (value instanceof String s) {
                String normalized = s.trim().toLowerCase(Locale.ROOT);
                if (normalized.equals("true") || normalized.equals("yes") || normalized.equals("y") || normalized.equals("1")) {
                    return (T) Boolean.TRUE;
                }
                if (normalized.equals("false") || normalized.equals("no") || normalized.equals("n") || normalized.equals("0")) {
                    return (T) Boolean.FALSE;
                }
            }
            return null;
        }

        if (type == UUID.class) {
            if (value instanceof UUID uuid) return (T) uuid;
            if (value instanceof String s) {
                try {
                    return (T) UUID.fromString(s.trim());
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        if (type == Duration.class) {
            switch (value) {
                case Duration d -> {
                    return (T) d;
                }
                case Number n -> {
                    return (T) Duration.ofSeconds(n.longValue());
                }
                case String s -> {
                    Duration parsed = parseDuration(s);
                    if (parsed != null) return (T) parsed;
                }
                default -> {
                }
            }
            return null;
        }

        if (type == PlatformId.class) {
            if (value instanceof PlatformId id) return (T) id;
            if (value instanceof String s) {
                try {
                    return (T) PlatformId.valueOf(s.trim().toUpperCase(Locale.ROOT));
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        if (type == MessageKey.class) {
            if (value instanceof MessageKey k) return (T) k;
            if (value instanceof String s) {
                String trimmed = s.trim();
                if (trimmed.isBlank()) return null;
                try {
                    return (T) MessageKey.of(trimmed);
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        if (type == RBlockPos.class) {
            switch (value) {
                case RBlockPos pos -> {
                    return (T) pos;
                }
                case Map<?, ?> map -> {
                    Integer x = coerce(mapGet(map, "x"), Integer.class);
                    Integer y = coerce(mapGet(map, "y"), Integer.class);
                    Integer z = coerce(mapGet(map, "z"), Integer.class);
                    if (x == null || y == null || z == null) return null;
                    return (T) new RBlockPos(x, y, z);
                }
                case String s -> {
                    RBlockPos parsed = parseBlockPos(s);
                    if (parsed != null) return (T) parsed;
                }
                default -> {
                }
            }
            return null;
        }

        if (type == RWorldRef.class) {
            switch (value) {
                case RWorldRef ref -> {
                    return (T) ref;
                }
                case Map<?, ?> map -> {
                    String name = coerce(mapGet(map, "name"), String.class);
                    String key = coerce(mapGet(map, "key"), String.class);
                    try {
                        return (T) new RWorldRef(name, key);
                    } catch (Exception ignored) {
                        return null;
                    }
                }
                case String s -> {
                    String id = s.trim();
                    if (id.isBlank()) return null;
                    try {
                        return (T) new RWorldRef(null, id);
                    } catch (Exception ignored) {
                        return null;
                    }
                }
                default -> {
                }
            }
            return null;
        }

        if (type == RLocation.class) {
            if (value instanceof RLocation loc) return (T) loc;
            if (value instanceof Map<?, ?> map) {
                RWorldRef world = coerce(mapGet(map, "world"), RWorldRef.class);
                Double x = coerce(mapGet(map, "x"), Double.class);
                Double y = coerce(mapGet(map, "y"), Double.class);
                Double z = coerce(mapGet(map, "z"), Double.class);
                Float yaw = coerce(mapGet(map, "yaw"), Float.class);
                Float pitch = coerce(mapGet(map, "pitch"), Float.class);
                if (world == null || x == null || y == null || z == null) return null;
                return (T) new RLocation(world, x, y, z, yaw != null ? yaw : 0F, pitch != null ? pitch : 0F);
            }
            return null;
        }

        if (type == Float.class) {
            if (value instanceof Number n) return (T) Float.valueOf(n.floatValue());
            if (value instanceof String s) {
                try {
                    return (T) Float.valueOf(Float.parseFloat(s.trim()));
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        if (type.isEnum()) {
            String raw = coerce(value, String.class);
            if (raw == null) return null;
            String normalized = raw.trim();
            if (normalized.isBlank()) return null;
            normalized = normalized.replace('-', '_').toUpperCase(Locale.ROOT);
            for (T c : type.getEnumConstants()) {
                if (((Enum<?>) c).name().equalsIgnoreCase(normalized)) return c;
            }
            return null;
        }

        if (type.isRecord() && value instanceof Map<?, ?> map) {
            return coerceRecord(map, type);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T coerceRecord(Map<?, ?> map, Class<T> recordType) {
        var components = recordType.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            paramTypes[i] = component.getType();
            Object raw = mapGet(map, component.getName());
            Object coerced = coerce(raw, (Class<Object>) component.getType());
            if (coerced == null && component.getType().isPrimitive()) return null;
            args[i] = coerced;
        }

        try {
            Constructor<T> ctor = recordType.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object mapGet(Map<?, ?> map, String key) {
        Object direct = map.get(key);
        if (direct != null || map.containsKey(key)) return direct;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() == null) continue;
            if (key.equalsIgnoreCase(String.valueOf(e.getKey()))) return e.getValue();
        }
        return null;
    }

    private static Duration parseDuration(String s) {
        String trimmed = s.trim();
        if (trimmed.isBlank()) return null;

        try {
            return Duration.parse(trimmed);
        } catch (Exception ignored) {
        }

        // Simple suffixes: 10s, 5m, 2h, 1d
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.length() < 2) return null;
        char unit = lower.charAt(lower.length() - 1);
        String numberPart = lower.substring(0, lower.length() - 1).trim();
        long amount;
        try {
            amount = Long.parseLong(numberPart);
        } catch (Exception ignored) {
            return null;
        }

        return switch (unit) {
            case 's' -> Duration.ofSeconds(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'd' -> Duration.ofDays(amount);
            default -> null;
        };
    }

    private static String formatDuration(Duration d) {
        if (d.isZero()) return "0s";
        if (!d.isNegative()) {
            long seconds = d.getSeconds();
            if (d.getNano() == 0) {
                if (seconds % 86400 == 0) return (seconds / 86400) + "d";
                if (seconds % 3600 == 0) return (seconds / 3600) + "h";
                if (seconds % 60 == 0) return (seconds / 60) + "m";
                return seconds + "s";
            }
        }
        return d.toString();
    }

    private static RBlockPos parseBlockPos(String s) {
        String trimmed = s.trim();
        if (trimmed.isBlank()) return null;
        String normalized = trimmed.replace(',', ' ');
        String[] parts = normalized.split("\\s+");
        if (parts.length != 3) return null;
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new RBlockPos(x, y, z);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object deepCopy(Object v) {
        if (v instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) continue;
                out.put(String.valueOf(e.getKey()), deepCopy(e.getValue()));
            }
            return out;
        }
        if (v instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object o : list) out.add(deepCopy(o));
            return out;
        }
        return v;
    }

    private static Set<String> flattenKeys(Map<String, Object> map) {
        Set<String> out = new LinkedHashSet<>();
        collectKeys(out, "", map);
        return out;
    }

    private static Object getAt(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (int i = 0; i < parts.length; i++) {
            if (!(current instanceof Map<?, ?> map)) return null;
            Object next = map.get(parts[i]);
            if (i == parts.length - 1) return next;
            current = next;
        }
        return null;
    }

    private static Map<String, String> parseComments(String content) {
        Map<String, String> out = new LinkedHashMap<>();

        List<Frame> stack = new ArrayList<>();
        Map<Integer, List<String>> pending = new LinkedHashMap<>();

        String[] lines = content.split("\\R", -1);
        for (String line : lines) {
            if (line.isBlank()) {
                pending.clear();
                continue;
            }

            int indent = countLeadingSpaces(line);
            String trimmed = line.stripLeading();

            if (trimmed.startsWith("#")) {
                String comment = trimmed.substring(1).stripLeading();
                pending.computeIfAbsent(indent, ignored -> new ArrayList<>()).add(comment);
                continue;
            }

            String key = parseKeyLine(trimmed);
            if (key == null) {
                pending.clear();
                continue;
            }

            while (!stack.isEmpty() && stack.getLast().indent >= indent) {
                stack.removeLast();
            }

            String path = buildPath(stack, key);
            List<String> pendingAtIndent = pending.remove(indent);
            if (pendingAtIndent != null && !pendingAtIndent.isEmpty()) {
                out.put(path, String.join("\n", pendingAtIndent).strip());
            }

            stack.add(new Frame(indent, key));
            pending.entrySet().removeIf(e -> e.getKey() > indent);
        }

        return out;
    }

    private static String insertComments(String dumped, Map<String, String> comments) {
        if (comments.isEmpty()) return dumped;

        List<Frame> stack = new ArrayList<>();
        StringBuilder out = new StringBuilder(dumped.length() + comments.size() * 16);

        String[] lines = dumped.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                out.append(line);
                if (i < lines.length - 1) out.append('\n');
                continue;
            }

            int indent = countLeadingSpaces(line);
            String trimmed = line.stripLeading();

            String key = parseKeyLine(trimmed);
            if (key != null) {
                while (!stack.isEmpty() && stack.getLast().indent >= indent) {
                    stack.removeLast();
                }

                String path = buildPath(stack, key);
                String comment = comments.get(path);
                if (comment != null && !comment.isBlank()) {
                    for (String commentLine : comment.split("\\R", -1)) {
                        out.append(" ".repeat(indent)).append("#");
                        if (!commentLine.isBlank()) out.append(" ").append(commentLine.strip());
                        out.append('\n');
                    }
                }

                stack.add(new Frame(indent, key));
            }

            out.append(line);
            if (i < lines.length - 1) out.append('\n');
        }

        return out.toString();
    }

    private record Frame(int indent, String key) {
    }

    private static int countLeadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }

    private static String buildPath(List<Frame> stack, String key) {
        if (stack.isEmpty()) return key;
        StringBuilder sb = new StringBuilder();
        for (Frame f : stack) {
            if (!sb.isEmpty()) sb.append('.');
            sb.append(f.key);
        }
        sb.append('.').append(key);
        return sb.toString();
    }

    private static String parseKeyLine(String trimmedLine) {
        if (trimmedLine.startsWith("-")) return null; // list item
        if (trimmedLine.startsWith("?")) return null; // complex key

        int colon = findColonOutsideQuotes(trimmedLine);
        if (colon <= 0) return null;

        String rawKey = trimmedLine.substring(0, colon).trim();
        if (rawKey.isEmpty()) return null;

        if ((rawKey.startsWith("'") && rawKey.endsWith("'")) || (rawKey.startsWith("\"") && rawKey.endsWith("\""))) {
            if (rawKey.length() >= 2) rawKey = rawKey.substring(1, rawKey.length() - 1);
        }

        return rawKey;
    }

    private static int findColonOutsideQuotes(String s) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == ':' && !inSingle && !inDouble) return i;
        }
        return -1;
    }

    private static String normalizeResourcePath(String path) {
        if (path.startsWith("/")) return path.substring(1);
        return path;
    }
}
