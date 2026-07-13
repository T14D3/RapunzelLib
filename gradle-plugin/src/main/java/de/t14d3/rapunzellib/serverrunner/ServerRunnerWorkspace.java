package de.t14d3.rapunzellib.serverrunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @deprecated Use {@link de.t14d3.rapunzellib.devrunner.DevRunnerWorkspace} instead.
 */
@Deprecated
record ServerRunnerWorkspace(Path baseDir, Path cacheDir, Path instancesDir) {
    static ServerRunnerWorkspace resolve(ServerRunnerMain.Config cfg) {
        Path baseDir = resolveBaseDir(cfg.baseDir());
        Path cacheDir = resolveChildDir(cfg.cacheDir(), baseDir.resolve("cache"), baseDir);
        Path instancesDir = resolveChildDir(cfg.instancesDir(), baseDir.resolve("instances"), baseDir);
        return new ServerRunnerWorkspace(baseDir, cacheDir, instancesDir);
    }

    void createDirectories() throws IOException {
        Files.createDirectories(baseDir);
        Files.createDirectories(cacheDir);
        Files.createDirectories(instancesDir);
    }

    private static Path resolveBaseDir(Path configuredBaseDir) {
        Path baseDir = configuredBaseDir != null ? configuredBaseDir : Path.of("run", "server-runner");
        return baseDir.toAbsolutePath().normalize();
    }

    private static Path resolveChildDir(Path configuredDir, Path defaultDir, Path baseDir) {
        Path resolved;
        if (configuredDir == null) {
            resolved = defaultDir;
        } else if (configuredDir.isAbsolute()) {
            resolved = configuredDir;
        } else {
            resolved = baseDir.resolve(configuredDir).normalize();
        }
        return resolved.toAbsolutePath().normalize();
    }
}
