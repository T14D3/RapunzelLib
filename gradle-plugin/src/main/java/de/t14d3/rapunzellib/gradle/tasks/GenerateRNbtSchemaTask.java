package de.t14d3.rapunzellib.gradle.tasks;

import de.t14d3.rapunzellib.gradle.nbt.RNbtSchemaGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@CacheableTask
public abstract class GenerateRNbtSchemaTask extends DefaultTask {
    private final ConfigurableFileCollection inputFiles = getProject().getObjects().fileCollection();

    public GenerateRNbtSchemaTask() {
        onlyIf("at least one RNbt schema input file exists", task -> inputFiles.getFiles().stream().anyMatch(File::isFile));
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    public ConfigurableFileCollection getInputFiles() {
        return inputFiles;
    }

    @Input
    public abstract Property<String> getPackageName();

    @Input
    public abstract Property<String> getClassName();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() {
        List<File> sourceInputs = new ArrayList<>();
        for (File file : inputFiles.getFiles()) {
            if (file.isFile()) {
                sourceInputs.add(file);
            }
        }
        sourceInputs.sort(Comparator.comparing(File::getAbsolutePath));
        if (sourceInputs.isEmpty()) {
            throw new GradleException(
                "No RNbt schema input files found. Configure rapunzellib.rNbtSchema.inputFiles with at least one file."
            );
        }

        var schema = RNbtSchemaGenerator.parseInputFiles(sourceInputs);
        String source = RNbtSchemaGenerator.renderJavaSource(getPackageName().get(), getClassName().get(), schema);

        File outputRoot = getOutputDir().get().getAsFile();
        getProject().delete(outputRoot);
        outputRoot.mkdirs();

        File targetFile = new File(new File(outputRoot, getPackageName().get().replace('.', '/')), getClassName().get() + ".java");
        targetFile.getParentFile().mkdirs();
        try {
            java.nio.file.Files.writeString(targetFile.toPath(), source, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new GradleException("Failed to write generated RNbt schema source.", ex);
        }

        getLogger().lifecycle(
            "Generated {} RNbt schema entries for '{}' at {}",
            schema.flattenedEntries().size(),
            schema.name(),
            getProject().relativePath(targetFile)
        );
    }
}
