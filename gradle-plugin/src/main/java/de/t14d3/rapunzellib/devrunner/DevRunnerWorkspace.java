package de.t14d3.rapunzellib.devrunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record DevRunnerWorkspace(Path baseDir, Path cacheDir, Path instancesDir) {

    public static DevRunnerWorkspace resolve(DevRunnerConfig cfg) {
        Path base = cfg.baseDir() != null ? cfg.baseDir() : Path.of("run", "devrunner");
        Path resolvedBase = base.toAbsolutePath().normalize();
        Path cache = cfg.cacheDir() != null ? resolveChild(cfg.cacheDir(), resolvedBase) : resolvedBase.resolve("cache");
        Path instances = cfg.instancesDir() != null ? resolveChild(cfg.instancesDir(), resolvedBase) : resolvedBase.resolve("instances");
        return new DevRunnerWorkspace(resolvedBase, cache, instances);
    }

    public void createDirectories() throws IOException {
        Files.createDirectories(baseDir);
        Files.createDirectories(cacheDir);
        Files.createDirectories(instancesDir);
    }

    public Path instanceDir(String name) throws IOException {
        Path dir = instancesDir.resolve(name);
        Files.createDirectories(dir);
        return dir;
    }

    private static Path resolveChild(Path configured, Path baseDir) {
        if (configured.isAbsolute()) return configured.toAbsolutePath().normalize();
        return baseDir.resolve(configured).normalize();
    }
}
