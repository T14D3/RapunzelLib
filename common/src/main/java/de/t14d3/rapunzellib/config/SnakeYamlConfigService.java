package de.t14d3.rapunzellib.config;

import de.t14d3.rapunzellib.context.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Implementation of {@link ConfigService} that creates {@link SnakeYamlConfig} instances.
 * <p>
 * Handles file creation, default resource copying, and directory creation before
 * delegating to {@link SnakeYamlConfig} for parsing and merging.
 */
public final class SnakeYamlConfigService implements ConfigService {
    /** Resource provider for loading default configs from the classpath */
    private final ResourceProvider resources;
    /** Logger for warnings and errors */
    private final Logger logger;

    /**
     * Creates a new config service.
     *
     * @param resources the resource provider for defaults
     * @param logger    the logger
     */
    public SnakeYamlConfigService(ResourceProvider resources, Logger logger) {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Loads a YAML config file, using its filename as the default resource path.
     *
     * @param file the path to the config file
     * @return the loaded YAML config
     */
    @Override
    public @NotNull YamlConfig load(@NotNull Path file) {
        return load(file, file.getFileName().toString());
    }

    /**
     * Loads a YAML config file with a specific default resource path.
     *
     * @param file                the path to the config file
     * @param defaultResourcePath the classpath resource path for default values
     * @return the loaded YAML config
     */
    @Override
    public @NotNull YamlConfig load(@NotNull Path file, @NotNull String defaultResourcePath) {
        Objects.requireNonNull(file, "file");

        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            logger.error("Failed to create config directory", e);
        }

        if (!Files.exists(file)) {
            copyDefaultTo(file, defaultResourcePath);
        }

        SnakeYamlConfig config = new SnakeYamlConfig(file, resources, logger, defaultResourcePath);
        config.reload();
        config.save();
        return config;
    }

    private void copyDefaultTo(Path targetFile, String defaultResourcePath) {
        if (defaultResourcePath == null || defaultResourcePath.isBlank()) {
            try {
                Files.createFile(targetFile);
            } catch (IOException ignored) {
            }
            return;
        }

        String normalized = normalizeResourcePath(defaultResourcePath);
        try (InputStream in = resources.open(normalized).orElse(null)) {
            if (in == null) {
                logger.warn(
                    "Bundled default resource '{}' not found on the classpath; creating empty config file {}",
                    normalized, targetFile
                );
                Files.createFile(targetFile);
                return;
            }
            Files.copy(in, targetFile);
        } catch (IOException e) {
            logger.warn("Failed to copy default resource {} to {}", normalized, targetFile, e);
            try {
                if (!Files.exists(targetFile)) Files.createFile(targetFile);
            } catch (IOException ignored) {
            }
        }
    }

    private static String normalizeResourcePath(String path) {
        if (path.startsWith("/")) return path.substring(1);
        return path;
    }
}
