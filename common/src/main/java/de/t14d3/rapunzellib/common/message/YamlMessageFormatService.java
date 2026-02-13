package de.t14d3.rapunzellib.common.message;

import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Message formatting service with YAML-backed templates, caching, and MiniMessage support.
 *
 * <p>This service loads message templates from YAML configuration files, compiles them using
 * MiniMessage for rich text formatting, and provides efficient placeholder replacement with
 * multi-layer caching for high-performance message rendering in multi-threaded environments.
 *
 * <p><strong>Template Compilation and Caching:</strong>
 * <p>Templates undergo a two-phase compilation process:
 * <ul>
 *   <li><strong>Parse Phase:</strong> MiniMessage templates are deserialized to {@link Component} objects</li>
 *   <li><strong>Extract Phase:</strong> Placeholder names (e.g., {@code <player>}, {@code <amount>}) are
 *       extracted from the parsed component tree and stored for efficient replacement</li>
 * </ul>
 * <p>Compiled templates are cached in an immutable {@link State} object that is atomically
 * replaced on reload, ensuring consistent reads during template updates.
 *
 * <p><strong>Placeholder Resolution:</strong>
 * <p>Placeholders are resolved in priority order:
 * <ol>
 *   <li>Component placeholders ({@code Component} objects passed directly)</li>
 *   <li>String placeholders (automatically wrapped in {@code Component.text()})</li>
 *   <li>Special {@code <prefix>} placeholder resolved from the configured prefix template</li>
 * </ol>
 * <p>Unresolved placeholders are left as-is in the output.
 *
 * <p><strong>Thread-Safe Rendering with Volatile Fields:</strong>
 * <p>This service uses a {@code volatile} {@link State} field for lock-free reads:
 * <ul>
 *   <li>All template reads access the volatile state reference for visibility across threads</li>
 *   <li>Individual {@link Template} instances use volatile fields for empty-render caches</li>
 *   <li>String-based placeholder combinations use synchronized LRU caches per template</li>
 *   <li>Prefix rendering uses atomic state capture to ensure consistency</li>
 * </ul>
 *
 * <p><strong>MiniMessage Integration:</strong>
 * <p>MiniMessage provides rich text formatting using tags like:
 * <ul>
 *   <li>Color codes: {@code <red>}, {@code <#RRGGBB>}</li>
 *   <li>Decorations: {@code <bold>}, {@code <italic>}, {@code <underlined>}</li>
 *   <li>Click/hover events: {@code <click:run_command:/help>}, {@code <hover:show_text:Info>}</li>
 *   <li>Gradients and more advanced formatting</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * YamlMessageFormatService service = new YamlMessageFormatService(
 *     configService, logger, Path.of("messages.yml"), "default-messages.yml"
 * );
 *
 * // Render with placeholders
 * Component msg = service.component("player.welcome",
 *     Placeholders.of("player", playerName, "server", serverName));
 *
 * // Check if key exists
 * if (service.contains("player.bye")) {
 *     Component bye = service.component("player.bye");
 * }
 * }</pre>
 *
 * <p>YAML configuration format:
 * <pre>
 * {@code
 * prefix: "<gray>[<blue>MyPlugin</blue>]</gray> "
 * player:
 *   welcome: "<prefix><green>Welcome <player> to <server>!</green>"
 *   bye: "<prefix><yellow>Goodbye <player>!</yellow>"
 * }
 * </pre>
 *
 * @implNote The service maintains three levels of caching: (1) empty render cache per template,
 *           (2) empty-with-prefix cache, and (3) LRU string-based placeholder cache.
 *           Cache size is bounded to prevent memory leaks in long-running servers.
 * @since 1.0
 * @see MessageFormatService
 * @see Placeholders
 * @see MiniMessage
 */
public final class YamlMessageFormatService implements MessageFormatService {
    private static final String PREFIX_KEY = "prefix";
    private static final int STRING_RENDER_CACHE_MAX_ENTRIES = 64;

    private final MiniMessage miniMessage;
    private final ConfigService configService;
    private final Logger logger;
    private final Path file;
    private final String defaultResourcePath;

    private volatile State state = new State(Map.of(), Set.of(), Component.empty());

    public YamlMessageFormatService(ConfigService configService, Logger logger, Path file, String defaultResourcePath) {
        this.miniMessage = MiniMessage.miniMessage();
        this.configService = Objects.requireNonNull(configService, "configService");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.file = Objects.requireNonNull(file, "file");
        this.defaultResourcePath = defaultResourcePath;
        reload();
    }

    @Override
    public void reload() {
        YamlConfig config = (defaultResourcePath == null || defaultResourcePath.isBlank())
            ? configService.load(file)
            : configService.load(file, defaultResourcePath);

        Map<String, Template> newTemplates = new LinkedHashMap<>();
        Set<String> allKeys = config.keys(true);

        for (String key : allKeys) {
            Object value = config.get(key);
            if (!(value instanceof String raw)) continue;
            newTemplates.put(key, parseTemplate(raw));
        }

        Template prefixTemplate = newTemplates.get(PREFIX_KEY);
        Component prefix = (prefixTemplate != null)
            ? render(prefixTemplate, Placeholders.empty(), Component.empty())
            : Component.empty();

        this.state = new State(
            Collections.unmodifiableMap(newTemplates),
            Collections.unmodifiableSet(newTemplates.keySet()),
            prefix
        );
    }

    @Override
    public boolean contains(@NotNull String key) {
        return state.templates.containsKey(key);
    }

    @Override
    public @NotNull Set<String> keys() {
        return state.keys;
    }

    @Override
    public @NotNull String raw(@NotNull String key) {
        Template t = state.templates.get(key);
        if (t == null) return key;
        return t.raw;
    }

    @Override
    public @NotNull Component component(@NotNull String key) {
        return component(key, Placeholders.empty());
    }

    @Override
    public @NotNull Component component(@NotNull String key, @NotNull Placeholders placeholders) {
        State state = this.state;
        Template t = state.templates.get(key);
        if (t == null) {
            return Component.text(key);
        }

        if (key.equals(PREFIX_KEY) && placeholders == Placeholders.empty()) {
            return state.prefix;
        }

        if (placeholders == Placeholders.empty() && state.prefix != Component.empty()) {
            Component cached = t.cachedEmptyWithPrefix;
            if (cached != null) return cached;

            Component rendered = render(t, placeholders, state.prefix);
            Component prefixed = state.prefix.append(rendered);
            t.cachedEmptyWithPrefix = prefixed;
            return prefixed;
        }

        Component rendered = render(t, placeholders, state.prefix);
        if (!key.equals(PREFIX_KEY) && state.prefix != Component.empty()) {
            return state.prefix.append(rendered);
        }
        return rendered;
    }

    private Component render(Template template, Placeholders placeholders, Component prefix) {
        if (template.placeholderOrder.length == 0) return template.component;

        if (placeholders == Placeholders.empty()) {
            Component cached = template.cachedEmpty;
            if (cached != null) return cached;
            Component rendered = renderUncached(template, placeholders, prefix);
            template.cachedEmpty = rendered;
            return rendered;
        }

        if (template.stringRenderCache != null && placeholders.components().isEmpty()) {
            String cacheKey = stringCacheKey(template, placeholders);
            if (cacheKey != null) {
                Component cached = template.stringRenderCache.get(cacheKey);
                if (cached != null) return cached;
                Component rendered = renderUncached(template, placeholders, prefix);
                template.stringRenderCache.put(cacheKey, rendered);
                return rendered;
            }
        }

        return renderUncached(template, placeholders, prefix);
    }

    private Component renderUncached(Template template, Placeholders placeholders, Component prefix) {
        Component out = template.component;

        for (String name : template.placeholderOrder) {
            Component replacement = placeholders.components().get(name);
            if (replacement == null) {
                String value = placeholders.strings().get(name);
                if (value != null) replacement = Component.text(value);
                else if (name.equals("prefix")) replacement = prefix;
                else continue;
            }

            out = out.replaceText(TextReplacementConfig.builder()
                .matchLiteral("<" + name + ">")
                .replacement(replacement)
                .build());
        }

        return out;
    }

    private static String stringCacheKey(Template template, Placeholders placeholders) {
        StringBuilder sb = new StringBuilder(template.placeholderOrder.length * 16);
        for (String name : template.placeholderOrder) {
            String value = placeholders.strings().get(name);
            if (value == null && name.equals("prefix")) {
                return null;
            }
            sb.append(name).append('\u0000');
            if (value == null) {
                sb.append(-1);
            } else {
                sb.append(value.length()).append(':').append(value);
            }
            sb.append('\u0000');
        }
        return sb.toString();
    }

    private Template parseTemplate(String raw) {
        Set<String> placeholderNames = new LinkedHashSet<>();

        Component parsed;
        try {
            parsed = miniMessage.deserialize(raw);
            extractPlaceholders(parsed, placeholderNames);
        } catch (Exception e) {
            logger.warn("Failed to parse MiniMessage template", e);
            parsed = Component.text(raw);
        }

        return new Template(raw, parsed, Collections.unmodifiableSet(placeholderNames));
    }

    private static void extractPlaceholders(Component root, Set<String> out) {
        if (root instanceof TextComponent text) {
            extractPlaceholdersFromText(text.content(), out);
        }
        for (Component child : root.children()) {
            extractPlaceholders(child, out);
        }
    }

    private static void extractPlaceholdersFromText(String text, Set<String> out) {
        if (text == null || text.isEmpty()) return;
        int i = 0;
        while (true) {
            int start = text.indexOf('<', i);
            if (start < 0) return;
            int end = text.indexOf('>', start + 1);
            if (end < 0) return;

            if (end == start + 1) {
                i = end + 1;
                continue;
            }

            String name = text.substring(start + 1, end);
            if (!name.isEmpty() && name.charAt(0) != '/' && isPlaceholderName(name)) {
                out.add(name);
            }

            i = end + 1;
        }
    }

    private static boolean isPlaceholderName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok =
                (c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') ||
                    c == '_' ||
                    c == '-' ||
                    c == '.';
            if (!ok) return false;
        }
        return true;
    }

    private static final class Template {
        private final String raw;
        private final Component component;
        private final String[] placeholderOrder;
        private final Map<String, Component> stringRenderCache;

        private volatile Component cachedEmpty;
        private volatile Component cachedEmptyWithPrefix;

        private Template(String raw, Component component, Set<String> placeholders) {
            this.raw = raw;
            this.component = component;
            this.placeholderOrder = placeholders.toArray(new String[0]);
            this.stringRenderCache = placeholders.isEmpty()
                ? null
                : Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
                        return size() > STRING_RENDER_CACHE_MAX_ENTRIES;
                    }
                });
        }
    }

    private record State(Map<String, Template> templates, Set<String> keys, Component prefix) {
    }
}
