package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * An immutable namespaced identifier composed of a namespace and a path.
 *
 * <p>Keys follow the format {@code namespace:path} and are used throughout
 * RapunzelLib to identify registries, entity types, item types, and other
 * game objects. Parsed keys are interned for efficiency.</p>
 */
public record RKey(@NotNull String namespace, @NotNull String path) implements Serializable {
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern PATH_PATTERN = Pattern.compile("[A-Za-z0-9_./-]+");
    private static final ConcurrentHashMap<String, RKey> INTERN = new ConcurrentHashMap<>();

    public RKey {
        namespace = requireSegment(namespace, "namespace", NAMESPACE_PATTERN);
        path = requireSegment(path, "path", PATH_PATTERN);
    }

    /**
     * Creates a key from a namespace and path.
     *
     * @param namespace the namespace (e.g. "minecraft", "myplugin")
     * @param path      the path within the namespace (e.g. "diamond", "entities/zombie")
     * @return a new RKey with the given namespace and path
     */
    public static @NotNull RKey of(@NotNull String namespace, @NotNull String path) {
        return new RKey(namespace, path);
    }

    /**
     * Creates a key by parsing a {@code namespace:path} string.
     *
     * <p>This is a convenience alias for {@link #parse(String)}.</p>
     *
     * @param value the key string in {@code namespace:path} format
     * @return the parsed RKey
     * @throws IllegalArgumentException if the string is not a valid key
     */
    public static @NotNull RKey of(@NotNull String value) {
        return parse(value);
    }

    /**
     * Parses a {@code namespace:path} string into an RKey, interning the result.
     *
     * <p>The result is interned so that subsequent parses of the same string
     * return the same instance. Throws on invalid input.</p>
     *
     * @param value the key string in {@code namespace:path} format
     * @return the interned RKey
     * @throws IllegalArgumentException if the string is not a valid key
     */
    public static @NotNull RKey parse(@NotNull String value) {
        String candidate = requireText(value, "value");
        return INTERN.computeIfAbsent(candidate, RKey::parseUncached);
    }

    private static @NotNull RKey parseUncached(@NotNull String candidate) {
        int separator = candidate.indexOf(':');
        if (separator <= 0 || separator == candidate.length() - 1 || candidate.indexOf(':', separator + 1) != -1) {
            throw new IllegalArgumentException("Invalid key '" + candidate + "'. Expected '<namespace>:<path>'");
        }
        return new RKey(candidate.substring(0, separator), candidate.substring(separator + 1));
    }

    /**
     * Tries to parse a {@code namespace:path} string, returning empty on failure.
     *
     * <p>Unlike {@link #parse(String)}, this method does not throw on invalid input
     * and does not intern the result.</p>
     *
     * @param value the key string to attempt parsing
     * @return an {@link Optional} containing the RKey if valid, or empty if invalid
     */
    public static @NotNull Optional<RKey> tryParse(@NotNull String value) {
        try {
            return Optional.of(parse(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Checks whether the given string is a valid key.
     *
     * @param value the string to validate
     * @return true if the string is a valid {@code namespace:path} key
     */
    public static boolean isValid(@NotNull String value) {
        return tryParse(value).isPresent();
    }

    /** Returns this key as a {@code namespace:path} string. */
    public @NotNull String asString() {
        return namespace + ":" + path;
    }

    @Override
    public @NotNull String toString() {
        return asString();
    }

    private static @NotNull String requireSegment(@NotNull String value, @NotNull String label, @NotNull Pattern pattern) {
        String candidate = requireText(value, label);
        if (!pattern.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Invalid " + label + " '" + candidate + "'");
        }
        return candidate;
    }

    private static @NotNull String requireText(@NotNull String value, @NotNull String label) {
        String candidate = Objects.requireNonNull(value, label).trim();
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return candidate;
    }
}
