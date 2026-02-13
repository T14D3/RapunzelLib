package de.t14d3.rapunzellib.gradle.catalog;

import org.gradle.api.GradleException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CatalogCodegenSupport {
    private static final Pattern KEY_PATTERN = Pattern.compile("([A-Za-z0-9_.-]+):([A-Za-z0-9_./-]+)");
    private static final Pattern IDENTIFIER_SEPARATOR = Pattern.compile("[^A-Za-z0-9]+");
    private static final Pattern NON_IDENTIFIER = Pattern.compile("[^A-Z0-9]+");
    private static final Pattern DUPLICATE_UNDERSCORES = Pattern.compile("_+");

    private CatalogCodegenSupport() {
    }

    public static List<NamespacedKeyEntry> parseNamespacedKeyInputs(Iterable<File> files, String catalogLabel) {
        LinkedHashSet<NamespacedKeyEntry> entries = new LinkedHashSet<>();
        for (File file : files) {
            List<String> lines;
            try {
                lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                throw new GradleException("Failed to read " + catalogLabel + " input " + file.getAbsolutePath(), ex);
            }
            for (int index = 0; index < lines.size(); index++) {
                int lineNumber = index + 1;
                String line = lines.get(index).trim();
                if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                var match = KEY_PATTERN.matcher(line);
                if (!match.matches()) {
                    throw new GradleException(
                        "Invalid " + catalogLabel + " entry at " + file.getAbsolutePath() + ":" + lineNumber
                            + ". Expected '<namespace>:<path>' but found '" + line + "'."
                    );
                }
                entries.add(new NamespacedKeyEntry(match.group(1), match.group(2)));
            }
        }

        List<NamespacedKeyEntry> result = new ArrayList<>(entries);
        result.sort(Comparator.comparing(NamespacedKeyEntry::namespace).thenComparing(NamespacedKeyEntry::path));
        return result;
    }

    public static Map<String, String> uniqueClassNames(List<String> namespaces) {
        Map<String, Integer> used = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (String namespace : namespaces) {
            names.put(namespace, uniqueName(sanitizeClassName(namespace, "Namespace"), used, ""));
        }
        return names;
    }

    public static Map<String, String> uniqueConstantNames(List<String> paths) {
        Map<String, Integer> used = new LinkedHashMap<>();
        List<String> sortedPaths = new ArrayList<>(paths);
        sortedPaths.sort(String::compareTo);
        Map<String, String> names = new LinkedHashMap<>();
        for (String path : sortedPaths) {
            names.put(path, uniqueName(sanitizeConstantName(path), used, "_"));
        }
        return names;
    }

    public static void requireValidPackageName(String packageName, String catalogLabel) {
        String trimmed = packageName.trim();
        if (trimmed.isEmpty()) {
            throw new GradleException(catalogLabel + " package name must not be blank.");
        }
        for (String segment : trimmed.split("\\.")) {
            requireValidJavaIdentifier(segment, "package segment", catalogLabel);
        }
    }

    public static void requireValidJavaIdentifier(String value, String label, String catalogLabel) {
        if (value.isEmpty()) {
            throw new GradleException(catalogLabel + " " + label + " must not be blank.");
        }
        if (!Character.isJavaIdentifierStart(value.charAt(0))) {
            throw new GradleException("Invalid " + catalogLabel + " " + label + " '" + value + "'.");
        }
        for (int index = 1; index < value.length(); index++) {
            if (!Character.isJavaIdentifierPart(value.charAt(index))) {
                throw new GradleException("Invalid " + catalogLabel + " " + label + " '" + value + "'.");
            }
        }
    }

    public static String requireValidQualifiedTypeName(String value, String label, String catalogLabel) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new GradleException(catalogLabel + " " + label + " must not be blank.");
        }
        for (String segment : trimmed.split("\\.")) {
            requireValidJavaIdentifier(segment, label, catalogLabel);
        }
        return trimmed;
    }

    public static String simpleTypeName(String qualifiedName) {
        int index = qualifiedName.lastIndexOf('.');
        return index >= 0 ? qualifiedName.substring(index + 1) : qualifiedName;
    }

    public static String javaString(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        builder.append('"');
        return builder.toString();
    }

    private static String uniqueName(String baseName, Map<String, Integer> used, String separator) {
        int count = used.getOrDefault(baseName, 0) + 1;
        used.put(baseName, count);
        return count == 1 ? baseName : baseName + separator + count;
    }

    private static String sanitizeClassName(String value, String fallback) {
        StringBuilder builder = new StringBuilder();
        for (String segment : IDENTIFIER_SEPARATOR.split(value)) {
            if (segment.isBlank()) {
                continue;
            }
            String normalized = segment.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(normalized.charAt(0)));
            builder.append(normalized.substring(1));
        }

        String candidate;
        if (builder.isEmpty()) {
            candidate = fallback;
        } else if (Character.isDigit(builder.charAt(0))) {
            candidate = fallback + builder;
        } else {
            candidate = builder.toString();
        }

        requireValidJavaIdentifier(candidate, "generated class name", "catalog");
        return candidate;
    }

    private static String sanitizeConstantName(String value) {
        String normalized = DUPLICATE_UNDERSCORES.matcher(
            NON_IDENTIFIER.matcher(value.toUpperCase(Locale.ROOT)).replaceAll("_").replaceAll("^_+|_+$", "")
        ).replaceAll("_");

        if (normalized.isBlank()) {
            return "KEY";
        }
        return Character.isDigit(normalized.charAt(0)) ? "KEY_" + normalized : normalized;
    }
}
