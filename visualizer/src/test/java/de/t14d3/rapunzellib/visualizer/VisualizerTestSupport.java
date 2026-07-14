package de.t14d3.rapunzellib.visualizer;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VisualizerTestSupport {
    private VisualizerTestSupport() {
    }

    public static BuildResult runGradle(Path projectDir, String... arguments) {
        java.util.List<String> allArgs = new java.util.ArrayList<>();
        allArgs.add("--stacktrace");
        allArgs.add("--no-configuration-cache");
        allArgs.addAll(java.util.Arrays.asList(arguments));
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(allArgs)
            .withPluginClasspath()
            .forwardOutput()
            .build();
    }

    public static void writeFile(Path baseDir, String relativePath, String contents) {
        try {
            Path target = baseDir.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.writeString(target, contents);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
