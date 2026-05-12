package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Defines which files are part of a sync group (relative to {@code rootDirectory}).
 *
 * <p>This is intentionally file-level diffing: only changed/new files are transferred.</p>
 */
public final class FileSyncSpec {
    private final Path rootDirectory;
    private final List<PathMatcher> includes;
    private final List<PathMatcher> excludes;
    private final boolean deleteExtraneous;

    private FileSyncSpec(
        @NotNull Path rootDirectory,
        @NotNull List<PathMatcher> includes,
        @NotNull List<PathMatcher> excludes,
        boolean deleteExtraneous
    ) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        this.includes = List.copyOf(includes);
        this.excludes = List.copyOf(excludes);
        this.deleteExtraneous = deleteExtraneous;
    }

    /**
     * Creates a new builder for the given root directory.
     *
     * @param rootDirectory the root directory for file sync
     * @return a new builder
     */
    public static Builder builder(Path rootDirectory) {
        return new Builder(rootDirectory);
    }

    /**
     * Returns the root directory for this sync spec.
     *
     * @return the root directory
     */
    public Path rootDirectory() {
        return rootDirectory;
    }

    /**
     * Returns whether extraneous files should be deleted.
     *
     * @return true if extraneous files are deleted
     */
    public boolean deleteExtraneous() {
        return deleteExtraneous;
    }

    /**
     * Checks whether the given relative path matches this spec's include/exclude rules.
     *
     * @param relativePath the relative path to check
     * @return true if the path should be synced
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean matches(Path relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");
        if (relativePath.isAbsolute() || relativePath.normalize().startsWith("..")) return false;

        boolean included = includes.isEmpty();
        for (PathMatcher include : includes) {
            if (include.matches(relativePath)) {
                included = true;
                break;
            }
        }
        if (!included) return false;

        for (PathMatcher exclude : excludes) {
            if (exclude.matches(relativePath)) return false;
        }
        return true;
    }

    /**
     * Computes a manifest of file paths to their SHA-256 hashes.
     *
     * @return map of file paths to hex-encoded SHA-256 hashes
     * @throws IOException if an I/O error occurs
     */
    public Map<String, String> computeManifest() throws IOException {
        if (!Files.exists(rootDirectory)) return Collections.emptyMap();

        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(rootDirectory)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                Path rel = rootDirectory.relativize(file);
                if (!matches(rel)) continue;
                out.put(toWirePath(rel), FileSyncUtil.sha256Hex(file));
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * Builds a ZIP archive containing the specified relative paths.
     *
     * @param relativePaths set of relative paths to include
     * @return the ZIP archive as a byte array
     * @throws IOException if an I/O error occurs
     */
    public byte[] buildZip(Set<String> relativePaths) throws IOException {
        Objects.requireNonNull(relativePaths, "relativePaths");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bytes)) {
            for (String wirePath : relativePaths) {
                if (wirePath == null || wirePath.isBlank()) continue;
                Path rel = fromWirePath(wirePath);
                if (!matches(rel)) continue;

                Path file = resolveSafe(rel);
                if (!Files.isRegularFile(file)) continue;

                ZipEntry entry = new ZipEntry(wirePath);
                entry.setTime(Files.getLastModifiedTime(file).toMillis());
                zos.putNextEntry(entry);
                try (InputStream in = Files.newInputStream(file)) {
                    in.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    /**
     * Applies a ZIP archive to the root directory, writing and deleting files as specified.
     *
     * @param zipBytes the ZIP archive bytes
     * @param deletePaths list of paths to delete
     * @return the result of the application
     * @throws IOException if an I/O error occurs
     */
    public ApplyResult applyZip(byte[] zipBytes, List<String> deletePaths) throws IOException {
        Objects.requireNonNull(zipBytes, "zipBytes");
        Objects.requireNonNull(deletePaths, "deletePaths");

        if (!Files.exists(rootDirectory)) {
            Files.createDirectories(rootDirectory);
        }

        Set<String> written = new LinkedHashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.isBlank() || name.startsWith("/") || name.startsWith("\\")) {
                    continue;
                }

                Path rel = fromWirePath(name);
                if (!matches(rel)) continue;

                if (entry.isDirectory()) {
                    Files.createDirectories(resolveSafe(rel));
                    continue;
                }

                Path target = resolveSafe(rel);
                Path parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                String prefix = target.getFileName().toString();
                if (prefix.length() < 3) {
                    prefix = (prefix + "___").substring(0, 3);
                }
                Path tmp = Files.createTempFile(parent != null ? parent : rootDirectory, prefix, ".tmp");
                try {
                    Files.copy(zis, tmp, StandardCopyOption.REPLACE_EXISTING);
                    moveAtomicOrReplace(tmp, target);
                } finally {
                    try {
                        Files.deleteIfExists(tmp);
                    } catch (Exception ignored) {
                    }
                }

                written.add(toWirePath(rel));
            }
        }

        List<String> deleted = new ArrayList<>();
        for (String wirePath : deletePaths) {
            if (wirePath == null || wirePath.isBlank()) continue;
            Path rel = fromWirePath(wirePath);
            if (!matches(rel)) continue;

            Path target = resolveSafe(rel);
            if (!Files.isRegularFile(target)) continue;
            Files.deleteIfExists(target);
            deleted.add(toWirePath(rel));
        }

        return new ApplyResult(List.copyOf(written), List.copyOf(deleted));
    }

    private static void moveAtomicOrReplace(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path resolveSafe(Path relativePath) {
        Path normalized = relativePath.normalize();
        if (normalized.isAbsolute() || normalized.startsWith("..")) {
            throw new IllegalArgumentException("Unsafe relative path: " + relativePath);
        }
        Path resolved = rootDirectory.resolve(normalized).normalize();
        if (!resolved.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("Path escapes root directory: " + relativePath);
        }
        return resolved;
    }

    private static String toWirePath(Path relativePath) {
        return relativePath.toString().replace('\\', '/');
    }

    private static Path fromWirePath(String wirePath) {
        String normalized = wirePath.replace('/', java.io.File.separatorChar);
        return Path.of(normalized).normalize();
    }

    /**
     * Result of applying a ZIP archive.
     *
     * @param writtenPaths paths that were written
     * @param deletedPaths paths that were deleted
     */
    public record ApplyResult(List<String> writtenPaths, List<String> deletedPaths) {
    }

    /**
     * Builder for {@link FileSyncSpec}.
     */
    public static final class Builder {
        private final Path rootDirectory;
        private final List<String> includes = new ArrayList<>();
        private final List<String> excludes = new ArrayList<>();
        private boolean deleteExtraneous;

        private Builder(Path rootDirectory) {
            this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        }

        /**
         * Adds a glob pattern for files to include.
         *
         * @param glob the glob pattern
         * @return this builder
         */
        public Builder includeGlob(String glob) {
            if (glob != null && !glob.isBlank()) {
                includes.add(glob);
            }
            return this;
        }

        /**
         * Adds a glob pattern for files to exclude.
         *
         * @param glob the glob pattern
         * @return this builder
         */
        public Builder excludeGlob(String glob) {
            if (glob != null && !glob.isBlank()) {
                excludes.add(glob);
            }
            return this;
        }

        /**
         * Sets whether extraneous files should be deleted.
         *
         * @param deleteExtraneous true to delete extraneous files
         * @return this builder
         */
        public Builder deleteExtraneous(boolean deleteExtraneous) {
            this.deleteExtraneous = deleteExtraneous;
            return this;
        }

        /**
         * Builds a new {@link FileSyncSpec}.
         *
         * @return the constructed spec
         */
        public FileSyncSpec build() {
            FileSystem fs = FileSystems.getDefault();
            List<PathMatcher> includeMatchers = new ArrayList<>();
            for (String glob : includes) {
                includeMatchers.add(fs.getPathMatcher("glob:" + normalizeGlob(glob)));
            }

            List<PathMatcher> excludeMatchers = new ArrayList<>();
            for (String glob : excludes) {
                excludeMatchers.add(fs.getPathMatcher("glob:" + normalizeGlob(glob)));
            }

            return new FileSyncSpec(rootDirectory, includeMatchers, excludeMatchers, deleteExtraneous);
        }

        private static String normalizeGlob(String glob) {
            return glob.replace('/', java.io.File.separatorChar);
        }
    }
}

