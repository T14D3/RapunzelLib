package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record RKey(@NotNull String namespace, @NotNull String path) implements Serializable {
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern PATH_PATTERN = Pattern.compile("[A-Za-z0-9_./-]+");

    public RKey {
        namespace = requireSegment(namespace, "namespace", NAMESPACE_PATTERN);
        path = requireSegment(path, "path", PATH_PATTERN);
    }

    public static @NotNull RKey of(@NotNull String namespace, @NotNull String path) {
        return new RKey(namespace, path);
    }

    public static @NotNull RKey of(@NotNull String value) {
        return parse(value);
    }

    public static @NotNull RKey parse(@NotNull String value) {
        String candidate = requireText(value, "value");
        int separator = candidate.indexOf(':');
        if (separator <= 0 || separator == candidate.length() - 1 || candidate.indexOf(':', separator + 1) != -1) {
            throw new IllegalArgumentException("Invalid key '" + candidate + "'. Expected '<namespace>:<path>'");
        }
        return new RKey(candidate.substring(0, separator), candidate.substring(separator + 1));
    }

    public static @NotNull Optional<RKey> tryParse(@NotNull String value) {
        try {
            return Optional.of(parse(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static boolean isValid(@NotNull String value) {
        return tryParse(value).isPresent();
    }

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
