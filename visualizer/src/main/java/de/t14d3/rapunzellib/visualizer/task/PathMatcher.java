package de.t14d3.rapunzellib.visualizer.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Matches file paths against a list of glob patterns.
 *
 * <p>Patterns use simple glob rules:
 * <ul>
 *   <li>{@code *} matches any characters within a single path segment</li>
 *   <li>{@code **} matches any number of path segments (including zero)</li>
 *   <li>{@code ?} matches a single character</li>
 * </ul>
 *
 * <p>Matching is case-sensitive and performed against the canonical path
 * of each file. The separator is always {@code /} regardless of platform.
 *
 * <p>This class is thread-safe (immutable after construction).
 */
public final class PathMatcher {
    private final List<Pattern> patterns;
    private int excludedCount;

    public PathMatcher(List<String> globs) {
        this.patterns = new ArrayList<>(globs == null ? 0 : globs.size());
        if (globs != null) {
            for (String glob : globs) {
                if (glob != null && !glob.isBlank()) {
                    patterns.add(globToRegex(glob.trim()));
                }
            }
        }
    }

    /**
     * @return {@code true} if the given file's canonical path matches any
     * of the configured patterns.
     */
    public boolean matches(File file) {
        if (patterns.isEmpty()) return false;
        String path = canonicalPath(file);
        for (Pattern p : patterns) {
            if (p.matcher(path).matches()) {
                excludedCount++;
                return true;
            }
        }
        return false;
    }

    /**
     * @return the number of files that have been matched (excluded) so far.
     */
    public int getExcludedCount() {
        return excludedCount;
    }

    /**
     * @return {@code true} if no patterns are configured (nothing to exclude).
     */
    public boolean isEmpty() {
        return patterns.isEmpty();
    }

    private static String canonicalPath(File f) {
        try {
            return f.getCanonicalPath().replace(File.separatorChar, '/');
        } catch (IOException e) {
            return f.getAbsolutePath().replace(File.separatorChar, '/');
        }
    }

    /**
     * Convert a glob pattern to a compiled regex.
     *
     * <p>{@code **} matches any sequence of characters (including path
     * separators), {@code *} matches any sequence except {@code /}, and
     * {@code ?} matches a single character except {@code /}.
     */
    private static Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder(glob.length() * 2);
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*' && i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                // ** matches anything including /
                sb.append(".*");
                i += 2;
            } else if (c == '*') {
                // * matches anything except /
                sb.append("[^/]*");
                i++;
            } else if (c == '?') {
                sb.append("[^/]");
                i++;
            } else if (c == '.' || c == '(' || c == ')' || c == '[' || c == ']' ||
                       c == '{' || c == '}' || c == '+' || c == '^' || c == '$' ||
                       c == '|' || c == '\\') {
                sb.append('\\').append(c);
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return Pattern.compile(sb.toString());
    }
}
