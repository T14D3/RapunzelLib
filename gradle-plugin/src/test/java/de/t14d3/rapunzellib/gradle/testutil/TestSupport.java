package de.t14d3.rapunzellib.gradle.testutil;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TestSupport {
    private TestSupport() {
    }

    public static BuildResult runGradle(Path projectDir, String... arguments) {
        List<String> allArguments = new ArrayList<>();
        allArguments.add("--stacktrace");
        allArguments.addAll(List.of(arguments));

        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(allArguments)
            .withPluginClasspath()
            .build();
    }

    public static void writeFile(Path baseDir, String relativePath, String contents) {
        try {
            Path target = baseDir.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.writeString(target, contents + "\n");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Path compileSources(Path baseDir, String fixtureName, Map<String, String> sources) {
        try {
            Path sourceRoot = baseDir.resolve("fixtures").resolve(fixtureName).resolve("src");
            Path classesRoot = baseDir.resolve("fixtures").resolve(fixtureName).resolve("classes");
            Files.createDirectories(sourceRoot);
            Files.createDirectories(classesRoot);
            List<String> sourceFiles = new ArrayList<>();
            for (Map.Entry<String, String> entry : sources.entrySet()) {
                Path sourceFile = sourceRoot.resolve(entry.getKey());
                Files.createDirectories(sourceFile.getParent());
                Files.writeString(sourceFile, entry.getValue());
                sourceFiles.add(toGradlePath(sourceFile));
            }
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("System Java compiler is unavailable.");
            }
            List<String> args = new ArrayList<>();
            args.add("-d");
            args.add(toGradlePath(classesRoot));
            args.addAll(sourceFiles);
            int result = compiler.run(null, null, null, args.toArray(String[]::new));
            if (result != 0) {
                throw new IllegalStateException("Fixture compilation failed for " + fixtureName + " with exit code " + result);
            }
            return classesRoot;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toGradlePath(Path path) {
        return path.toString().replace(File.separatorChar, '/');
    }
}
