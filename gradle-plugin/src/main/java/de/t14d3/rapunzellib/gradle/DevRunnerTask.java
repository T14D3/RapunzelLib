package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.devrunner.DevRunnerConfig;
import de.t14d3.rapunzellib.devrunner.DevRunnerConfigParser;
import de.t14d3.rapunzellib.devrunner.DevRunnerMain;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@DisableCachingByDefault
public abstract class DevRunnerTask extends DefaultTask {

    private DevRunnerExtension extension;

    @Inject
    public DevRunnerTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    public void setExtension(DevRunnerExtension extension) {
        this.extension = extension;
    }

    @Internal
    public DevRunnerExtension getExtension() {
        return extension;
    }

    @OutputDirectory
    public DirectoryProperty getBaseDir() {
        return extension.getBaseDir();
    }

    @TaskAction
    public void run() {
        DevRunnerConfig config = buildConfig();

        Path configFile;
        try {
            Files.createDirectories(config.baseDir());
            configFile = Files.createTempFile(config.baseDir(), "devrunner-config", ".json");
            DevRunnerConfigParser.writeJson(config, configFile);
        } catch (IOException e) {
            throw new GradleException("Failed to write DevRunner config: " + e.getMessage(), e);
        }

        int exitCode = DevRunnerMain.run(new String[]{"--config", configFile.toString()});

        try {
            Files.deleteIfExists(configFile);
        } catch (IOException ignored) {
        }

        if (exitCode != 0) {
            throw new GradleException("DevRunner exited with code " + exitCode);
        }
    }

    private DevRunnerConfig buildConfig() {
        String javaBin = extension.getJavaBin().getOrElse("");
        List<String> jvmArgs = extension.getJvmArgs().getOrElse(List.of());
        boolean jfrEnabled = Boolean.TRUE.equals(extension.getJfrEnabled().getOrElse(false));
        String jfrSettings = extension.getJfrSettings().getOrElse("profile");
        boolean allowDirectConnections = Boolean.TRUE.equals(extension.getAllowDirectConnections().getOrElse(false));

        Path baseDirPath = extension.getBaseDir().get().getAsFile().toPath().toAbsolutePath().normalize();

        // Build servers
        Map<String, DevRunnerConfig.ServerSpec> servers = new LinkedHashMap<>();
        for (DevRunnerExtension.ServerSpecConfig spec : extension.getServers()) {
            Path pluginJar = spec.getPluginJar().isPresent()
                ? spec.getPluginJar().get().getAsFile().toPath()
                : null;

            List<Path> extraPlugins = new ArrayList<>();
            for (String p : spec.getExtraPlugins().getOrElse(List.of())) {
                extraPlugins.add(Path.of(p));
            }

            servers.put(spec.getName(), new DevRunnerConfig.ServerSpec(
                spec.getPlatform().getOrElse("paper"),
                spec.getVersion().getOrElse("latest"),
                spec.getPort().getOrElse(25565),
                pluginJar,
                extraPlugins,
                spec.getProperties().getOrElse(Map.of())
            ));
        }

        // Apply plugin JAR wiring from extension
        wirePluginJars(servers);

        // Build services
        Map<String, DevRunnerConfig.ServiceSpec> services = new LinkedHashMap<>();
        for (DevRunnerExtension.ServiceSpecConfig spec : extension.getServices()) {
            services.put(spec.getName(), new DevRunnerConfig.ServiceSpec(
                spec.getType().getOrElse("custom"),
                spec.getImage().getOrNull(),
                spec.getPorts().getOrElse(Map.of()),
                spec.getEnv().getOrElse(Map.of()),
                spec.getContainerName().getOrNull()
            ));
        }

        // LiveTests
        DevRunnerExtension.LiveTestsConfig lt = extension.getLiveTests();
        DevRunnerConfig.LiveTestConfig liveTests = new DevRunnerConfig.LiveTestConfig(
            Boolean.TRUE.equals(lt.getEnabled().getOrElse(false)),
            Boolean.TRUE.equals(lt.getAutoRun().getOrElse(true)),
            lt.getTestSourceDir().isPresent() ? lt.getTestSourceDir().get().getAsFile().toPath() : null,
            lt.getTestPackages().getOrElse(List.of()),
            lt.getTimeoutMs().getOrElse(30_000L),
            lt.getRunTimeoutMs().getOrElse(300_000L)
        );

        // File overrides
        Map<String, Map<String, String>> fileOverrides = extension.getFileOverrides().getOrElse(Map.of());

        return new DevRunnerConfig(
            javaBin, jvmArgs, baseDirPath, baseDirPath.resolve("cache"), baseDirPath.resolve("instances"),
            servers, services, liveTests, List.of(), fileOverrides, jfrEnabled, jfrSettings,
            allowDirectConnections
        );
    }

    private void wirePluginJars(Map<String, DevRunnerConfig.ServerSpec> servers) {
        DevRunnerExtension.PluginJarContainer pjc = extension.getPluginJars();

        for (var entry : servers.entrySet()) {
            DevRunnerConfig.ServerSpec spec = entry.getValue();
            if (spec.pluginJar() != null) continue;

            File jarFile = switch (spec.platform()) {
                case "paper" -> pjc.getPaper().isPresent() ? pjc.getPaper().get().getAsFile() : null;
                case "velocity" -> pjc.getVelocity().isPresent() ? pjc.getVelocity().get().getAsFile() : null;
                case "fabric" -> pjc.getFabric().isPresent() ? pjc.getFabric().get().getAsFile() : null;
                case "neoforge" -> pjc.getNeoForge().isPresent() ? pjc.getNeoForge().get().getAsFile() : null;
                case "sponge" -> pjc.getSponge().isPresent() ? pjc.getSponge().get().getAsFile() : null;
                default -> null;
            };

            if (jarFile != null && jarFile.isFile()) {
                servers.put(entry.getKey(), new DevRunnerConfig.ServerSpec(
                    spec.platform(), spec.version(), spec.port(),
                    jarFile.toPath(), spec.extraPlugins(), spec.properties()
                ));
            }
        }
    }
}
