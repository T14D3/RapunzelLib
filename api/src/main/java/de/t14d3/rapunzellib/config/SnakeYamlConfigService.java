package de.t14d3.rapunzellib.config;

import de.t14d3.rapunzellib.context.ResourceProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class SnakeYamlConfigService implements ConfigService {
    private final ResourceProvider resources;
    private final Logger logger;

    public SnakeYamlConfigService(ResourceProvider resources, Logger logger) {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public YamlConfig load(Path file) {
        return load(file, null);
    }

    @Override
    public YamlConfig load(Path file, String defaultResourcePath) {
        Objects.requireNonNull(file, "file");

        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            logger.error("Failed to create config directory: {}", e.getMessage());
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
                Files.createFile(targetFile);
                return;
            }
            Files.copy(in, targetFile);
        } catch (IOException e) {
            logger.warn("Failed to copy default resource {} to {}: {}", normalized, targetFile, e.getMessage());
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

