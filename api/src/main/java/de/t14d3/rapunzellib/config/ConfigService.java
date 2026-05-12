package de.t14d3.rapunzellib.config;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Service for loading and managing YAML configuration files.
 */
public interface ConfigService {
    /**
     * Loads a YAML config file from the given path.
     *
     * @param file the on-disk YAML file path
     * @return loaded YAML configuration
     */
    @NotNull YamlConfig load(@NotNull Path file);

    /**
     * Loads a YAML config file and merges missing keys/comments from a classpath default.
     *
     * @param file the on-disk YAML file
     * @param defaultResourcePath classpath resource path (e.g. {@code "config.yml"})
     * @return loaded YAML configuration with defaults merged
     */
    @NotNull YamlConfig load(@NotNull Path file, @NotNull String defaultResourcePath);
}

