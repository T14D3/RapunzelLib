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
 * <p><strong>Implementation note:</strong> The service maintains three levels of caching:
 * (1) empty render cache per template, (2) empty-with-prefix cache, and (3) LRU string-based
 * placeholder cache. Cache size is bounded to prevent memory leaks in long-running servers.
 * @since 1.0
 * @see MessageFormatService
 * @see Placeholders
 * @see MiniMessage
 */
public final class YamlMessageFormatService implements MessageFormatService {
    /** YAML config key for the prefix template */
    private static final String PREFIX_KEY = "prefix";
    /** Maximum entries in the string render LRU cache per template */
    private static final int STRING_RENDER_CACHE_MAX_ENTRIES = 64;

    /** MiniMessage instance for template deserialization */
    private final MiniMessage miniMessage;
    /** Config service for loading the YAML file */
    private final ConfigService configService;
    /** Logger for parsing warnings */
    private final Logger logger;
    /** Path to the messages YAML file */
    private final Path file;
    /** Classpath resource path for default messages */
    private final String defaultResourcePath;

    /** Current immutable state atomically replaced on reload */
    private volatile State state = new State(Map.of(), Set.of(), Component.empty());

    /**
     * Creates a new YAML message format service and loads templates immediately.
     *
     * @param configService       the config service for loading the YAML file
     * @param logger              the logger
     * @param file                the path to the messages YAML file
     * @param defaultResourcePath the classpath resource path for default messages
     */
    public YamlMessageFormatService(ConfigService configService, Logger logger, Path file, String defaultResourcePath) {
        this.miniMessage = MiniMessage.miniMessage();
        this.configService = Objects.requireNonNull(configService, "configService");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.file = Objects.requireNonNull(file, "file");
        this.defaultResourcePath = defaultResourcePath;
        reload();
    }

    /**
     * Reloads all message templates from the YAML file, rebuilding the state atomically.
     */
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

    /**
     * Checks whether the given message key exists.
     *
     * @param key the message key
     * @return true if the key exists
     */
    @Override
    public boolean contains(@NotNull String key) {
        return state.templates.containsKey(key);
    }

    /**
     * Returns all message keys.
     *
     * @return an immutable set of keys
     */
    @Override
    public @NotNull Set<String> keys() {
        return state.keys;
    }

    /**
     * Returns the raw template string for the given key.
     *
     * @param key the message key
     * @return the raw template, or the key itself if not found
     */
    @Override
    public @NotNull String raw(@NotNull String key) {
        Template t = state.templates.get(key);
        if (t == null) return key;
        return t.raw;
    }

    /**
     * Renders a message component without placeholders.
     *
     * @param key the message key
     * @return the rendered component
     */
    @Override
    public @NotNull Component component(@NotNull String key) {
        return component(key, Placeholders.empty());
    }

    /**
     * Renders a message component with the given placeholders.
     *
     * @param key          the message key
     * @param placeholders the placeholder values
     * @return the rendered component
     */
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

    /**
     * Renders a template with placeholders, using caching for empty and string-only placeholders.
     *
     * @param template    the template to render
     * @param placeholders the placeholder values
     * @param prefix      the prefix component to use for {@code <prefix>} substitution
     * @return the rendered component
     */
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

    /**
     * Performs placeholder substitution without caching.
     *
     * @param template    the template to render
     * @param placeholders the placeholder values
     * @param prefix      the prefix component for {@code <prefix>} substitution
     * @return the rendered component
     */
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

    /**
     * Builds a cache key string from template placeholder order and string values.
     *
     * @param template    the template
     * @param placeholders the placeholder values
     * @return a cache key string, or null if the prefix placeholder requires dynamic resolution
     */
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

    /**
     * Parses a raw MiniMessage template string into a {@link Template} with extracted placeholders.
     *
     * @param raw the raw template string
     * @return the compiled template
     */
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

    /**
     * Recursively extracts placeholder names from a component tree.
     *
     * @param root the root component
     * @param out  the set to collect placeholder names into
     */
    private static void extractPlaceholders(Component root, Set<String> out) {
        if (root instanceof TextComponent text) {
            extractPlaceholdersFromText(text.content(), out);
        }
        for (Component child : root.children()) {
            extractPlaceholders(child, out);
        }
    }

    /**
     * Extracts placeholder names from text content by finding {@code <name>} patterns.
     *
     * @param text the text content
     * @param out  the set to collect placeholder names into
     */
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

    /**
     * Checks whether a string is a valid placeholder name (alphanumeric, underscore, hyphen, dot).
     *
     * @param name the candidate name
     * @return true if valid
     */
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

    /**
     * Compiled message template with parsed MiniMessage component and placeholder metadata.
     * <p>
     * Maintains thread-safe caches for empty renders and string-only placeholder combinations.
     * The {@code cachedEmpty} and {@code cachedEmptyWithPrefix} fields are volatile for
     * lock-free publication across threads.
     */
    private static final class Template {
        /** The raw template string */
        private final String raw;
        /** The parsed MiniMessage component */
        private final Component component;
        /** Ordered array of placeholder names as they appear in the template */
        private final String[] placeholderOrder;
        /** LRU cache for string-based placeholder combinations (null if no placeholders) */
        private final Map<String, Component> stringRenderCache;

        /** Cached render result for empty placeholders (volatile for visibility) */
        private volatile Component cachedEmpty;
        /** Cached render result for empty placeholders with prefix (volatile for visibility) */
        private volatile Component cachedEmptyWithPrefix;

        /**
         * Creates a compiled template.
         *
         * @param raw          the raw template string
         * @param component    the parsed MiniMessage component
         * @param placeholders the set of extracted placeholder names
         */
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

    /**
     * Immutable snapshot of all templates, keys, and the resolved prefix component.
     * Atomically replaced on reload for consistent reads.
     */
    private record State(Map<String, Template> templates, Set<String> keys, Component prefix) {
    }
}
