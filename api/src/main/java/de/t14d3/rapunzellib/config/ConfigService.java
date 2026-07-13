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
     * @param file the path to the YAML file
     * @return the loaded YamlConfig
     */
    @NotNull YamlConfig load(@NotNull Path file);

    /**
     * Loads a YAML config file and merges missing keys/comments from a classpath default.
     *
     * <p>Keys present in the classpath default but missing from the file are
     * filled in with their default values and comments. This ensures the file
     * always has up-to-date defaults without overwriting user changes.</p>
     *
     * @param file                the path to the YAML file on disk
     * @param defaultResourcePath the classpath resource path to the default YAML (e.g. "/config-defaults.yml")
     * @return the loaded YamlConfig with defaults merged
     */
    @NotNull YamlConfig load(@NotNull Path file, @NotNull String defaultResourcePath);
}

