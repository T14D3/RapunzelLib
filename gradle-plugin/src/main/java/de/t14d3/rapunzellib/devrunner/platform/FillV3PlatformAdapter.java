package de.t14d3.rapunzellib.devrunner.platform;

import de.t14d3.rapunzellib.serverrunner.FillV3Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public abstract class FillV3PlatformAdapter implements PlatformAdapter {

    protected final FillV3Client fillClient = new FillV3Client();

    @Override
    public List<String> defaultJvmArgs(Path instanceDir) {
        return List.of();
    }

    @Override
    public void installPlugin(Path instanceDir, Path pluginJar) throws IOException {
        Path modDir = instanceDir.resolve(modDirectory());
        Files.createDirectories(modDir);
        Files.copy(pluginJar, modDir.resolve(pluginJar.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    protected Path resolveJar(String version) throws IOException, InterruptedException {
        String fillProject = fillProject();
        if (fillProject == null) {
            throw new IllegalStateException("No Fill v3 project for platform " + key());
        }
        String resolvedVersion = (version == null || version.isBlank() || version.equalsIgnoreCase("latest"))
            ? defaultVersion()
            : version;
        FillV3Client.ResolvedBuild build = fillClient.resolveLatestBuild(fillProject, resolvedVersion);
        return fillClient.downloadJar(fillProject, resolvedVersion, build, Path.of("run", "devrunner", "cache"));
    }
}
