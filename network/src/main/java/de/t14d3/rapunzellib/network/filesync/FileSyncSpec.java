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

    public static Builder builder(Path rootDirectory) {
        return new Builder(rootDirectory);
    }

    public Path rootDirectory() {
        return rootDirectory;
    }

    public boolean deleteExtraneous() {
        return deleteExtraneous;
    }

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

    public record ApplyResult(List<String> writtenPaths, List<String> deletedPaths) {
    }

    public static final class Builder {
        private final Path rootDirectory;
        private final List<String> includes = new ArrayList<>();
        private final List<String> excludes = new ArrayList<>();
        private boolean deleteExtraneous;

        private Builder(Path rootDirectory) {
            this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        }

        public Builder includeGlob(String glob) {
            if (glob != null && !glob.isBlank()) {
                includes.add(glob);
            }
            return this;
        }

        public Builder excludeGlob(String glob) {
            if (glob != null && !glob.isBlank()) {
                excludes.add(glob);
            }
            return this;
        }

        public Builder deleteExtraneous(boolean deleteExtraneous) {
            this.deleteExtraneous = deleteExtraneous;
            return this;
        }

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

