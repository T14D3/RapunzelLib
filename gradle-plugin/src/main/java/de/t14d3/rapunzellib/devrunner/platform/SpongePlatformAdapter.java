package de.t14d3.rapunzellib.devrunner.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SpongePlatformAdapter implements PlatformAdapter {

    @Override
    public String key() {
        return "sponge";
    }

    @Override
    public String fillProject() {
        return null;
    }

    @Override
    public String defaultVersion() {
        return "latest";
    }

    @Override
    public String modDirectory() {
        return "mods";
    }

    @Override
    public List<String> bootstrapFiles() {
        return List.of();
    }

    @Override
    public List<String> defaultJvmArgs(Path instanceDir) {
        return List.of();
    }

    @Override
    public List<String> programArgs() {
        return List.of("nogui");
    }

    @Override
    public String shutdownCommand() {
        return "stop";
    }

    @Override
    public void bootstrapOnce(BootstrapContext ctx) throws Exception {
        // Sponge servers generate their config on first run automatically
    }

    @Override
    public void postBootstrap(PostBootstrapContext ctx) throws Exception {
        // No special post-bootstrap needed for Sponge
    }

    @Override
    public void installPlugin(Path instanceDir, Path pluginJar) throws IOException {
        Path modsDir = instanceDir.resolve("mods");
        Files.createDirectories(modsDir);
        Files.copy(pluginJar, modsDir.resolve(pluginJar.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
