package de.t14d3.rapunzellib.config;

import java.nio.file.Path;

public interface ConfigService {
    YamlConfig load(Path file);

    /**
     * Loads a YAML config file and merges missing keys/comments from a classpath default.
     *
     * @param file the on-disk YAML file
     * @param defaultResourcePath classpath resource path (e.g. {@code "config.yml"})
     */
    YamlConfig load(Path file, String defaultResourcePath);
}

