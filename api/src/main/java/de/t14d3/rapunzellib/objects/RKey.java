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
 * game objects.</p>
 *
 * <p><b>Interning.</b> Every {@code RKey} obtained through any public factory
 * ({@link #of(String, String)}, {@link #of(String)}, {@link #parse(String)},
 * {@link #tryParse(String)}) is interned: for any given
 * {@code (namespace, path)} pair there is at most one {@code RKey} instance,
 * so {@code ==} comparison is sound for keys that passed through any public
 * entry point. {@link #equals(Object)} (inherited from the record contract)
 * remains correct regardless of interning and should be used by any consumer
 * that may receive keys from arbitrary sources (e.g. deserialization paths
 * outside this class). Interning is purely an identity optimization layered
 * on top of the {@code equals}-based semantic contract.</p>
 *
 * <p><b>Serialization.</b> {@code RKey} implements {@link Serializable}.
 * Deserialized instances are collapsed back into the intern table via
 * {@link #readResolve()}, so a key round-tripped through a network packet or
 * a persistent store is reference-identical to any other key with the same
 * logical value.</p>
 */
public record RKey(@NotNull String namespace, @NotNull String path) implements Serializable {
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern PATH_PATTERN = Pattern.compile("[A-Za-z0-9_./-]+");
    /**
     * Intern table keyed by the canonical (validated, trimmed) component pair.
     * The lookup key is a transient {@code RKey} constructed solely for the
     * cache probe (the canonical constructor both validates and trims); the
     * stored value is the canonical long-lived instance returned to callers.
     */
    private static final ConcurrentHashMap<RKey, RKey> INTERN = new ConcurrentHashMap<>();

    public RKey {
        namespace = requireSegment(namespace, "namespace", NAMESPACE_PATTERN);
        path = requireSegment(path, "path", PATH_PATTERN);
    }

    /**
     * Creates a key from a namespace and path. The result is interned:
     * repeated calls with the same (post-trim) component pair return the
     * same {@code RKey} instance.
     *
     * @param namespace the namespace (e.g. "minecraft", "myplugin")
     * @param path      the path within the namespace (e.g. "diamond", "entities/zombie")
     * @return the interned {@code RKey}
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IllegalArgumentException if either segment is blank or fails validation
     */
    public static @NotNull RKey of(@NotNull String namespace, @NotNull String path) {
        // The probe goes through the canonical constructor, which validates
        // and trims; computeIfAbsent runs the identity function only on miss,
        // so the long-lived canonical instance is the first-seen probe itself.
        RKey probe = new RKey(namespace, path);
        return INTERN.computeIfAbsent(probe, k -> k);
    }

    /**
     * Creates a key by parsing a {@code namespace:path} string. The result
     * is interned (see {@linkplain RKey class docs}).
     *
     * <p>This is a convenience alias for {@link #parse(String)}.</p>
     *
     * @param value the key string in {@code namespace:path} format
     * @return the interned {@code RKey}
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if the string is not a valid key
     */
    public static @NotNull RKey of(@NotNull String value) {
        return parse(value);
    }

    /**
     * Parses a {@code namespace:path} string into an {@code RKey}, interning
     * the result. The canonical instance for the parsed key is returned on
     * every subsequent call with the same (post-trim) component pair.
     *
     * @param value the key string in {@code namespace:path} format
     * @return the interned {@code RKey}
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if the string is not a valid key
     */
    public static @NotNull RKey parse(@NotNull String value) {
        String candidate = requireText(value, "value");
        int separator = candidate.indexOf(':');
        if (separator <= 0 || separator == candidate.length() - 1 || candidate.indexOf(':', separator + 1) != -1) {
            throw new IllegalArgumentException("Invalid key '" + candidate + "'. Expected '<namespace>:<path>'");
        }
        return of(candidate.substring(0, separator), candidate.substring(separator + 1));
    }

    /**
     * Tries to parse a {@code namespace:path} string, returning empty on
     * failure (including invalid segment characters or a malformed
     * {@code namespace:path} structure). The returned key (when present)
     * is interned, matching {@link #parse(String)}.
     *
     * <p>A {@code null} argument is propagated as a {@link NullPointerException}
     * rather than swallowed: it is a programmer error, not a parse failure.</p>
     *
     * @param value the key string to attempt parsing
     * @return an {@link Optional} containing the interned {@code RKey} if
     *         valid, or empty if the value is not a well-formed key
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static @NotNull Optional<RKey> tryParse(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        String candidate = value.trim();
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        int separator = candidate.indexOf(':');
        if (separator <= 0 || separator == candidate.length() - 1 || candidate.indexOf(':', separator + 1) != -1) {
            return Optional.empty();
        }
        try {
            return Optional.of(of(candidate.substring(0, separator), candidate.substring(separator + 1)));
        } catch (IllegalArgumentException ex) {
            // Segment-pattern validation rejected the parsed parts.
            return Optional.empty();
        }
    }

    /**
     * Checks whether the given string is a valid key. Does not intern the
     * result (the candidate key is thrown away, not stored).
     *
     * @param value the string to validate
     * @return true if the string is a valid {@code namespace:path} key
     * @throws NullPointerException if {@code value} is {@code null}
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

    /**
     * Collapses deserialized {@code RKey} instances back into the intern
     * table so that {@code ==} identity is preserved across serialization
     * boundaries (packets, persisted configs, world saves). Invoked by
     * the Java serialization runtime after the record's components have
     * been populated.
     */
    private Object readResolve() {
        return of(namespace, path);
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
