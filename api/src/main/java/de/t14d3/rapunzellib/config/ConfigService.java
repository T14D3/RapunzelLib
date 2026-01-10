package de.t14d3.rapunzellib.config;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface ConfigService {
    @NotNull YamlConfig load(@NotNull Path file);

    /**
     * Loads a YAML config file and merges missing keys/comments from a classpath default.
     *
     * @param file the on-disk YAML file
     * @param defaultResourcePath classpath resource path (e.g. {@code "config.yml"})
     */
    @NotNull YamlConfig load(@NotNull Path file, @NotNull String defaultResourcePath);
}

