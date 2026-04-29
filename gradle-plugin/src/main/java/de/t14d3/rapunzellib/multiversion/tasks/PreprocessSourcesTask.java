package de.t14d3.rapunzellib.multiversion.tasks;

import de.t14d3.rapunzellib.multiversion.SourcePreprocessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@DisableCachingByDefault
public abstract class PreprocessSourcesTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract Property<File> getSourceDir();

    @OutputDirectory
    public abstract Property<File> getOutputDir();

    @Input
    public abstract Property<String> getTargetVersion();

    @TaskAction
    public void preprocess() throws IOException {
        File sourceDir = getSourceDir().get();
        File outputDir = getOutputDir().get();
        String targetVersion = getTargetVersion().get();

        if (!sourceDir.exists()) {
            getLogger().warn("Source directory does not exist: {}", sourceDir);
            return;
        }

        FileTree sourceFiles = getProject()
                .fileTree(sourceDir)
                .matching(spec -> spec.include("**/*.java", "**/*.kt"));

        SourcePreprocessor preprocessor = new SourcePreprocessor(targetVersion);

        int processedCount = 0;

        for (File sourceFile : sourceFiles) {
            String relativePath = sourceDir.toPath().relativize(sourceFile.toPath()).toString();
            File outputFile = new File(outputDir, relativePath);

            File parent = outputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            String content = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
            String processed = preprocessor.process(content);

            Files.writeString(outputFile.toPath(), processed, StandardCharsets.UTF_8);
            processedCount++;
        }

        getLogger().info("Preprocessed {} files for version {}", processedCount, targetVersion);
    }
}