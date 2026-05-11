package de.t14d3.rapunzellib.gradle.tasks;

import de.t14d3.rapunzellib.gradle.display.DisplayMetadataGenerator;
import de.t14d3.rapunzellib.gradle.display.DisplayMetadataSpec;
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
import java.util.List;

@CacheableTask
public abstract class GenerateDisplayMetadataTask extends DefaultTask {
    private final ConfigurableFileCollection minecraftClasspath = getProject().getObjects().fileCollection();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    public ConfigurableFileCollection getMinecraftClasspath() {
        return minecraftClasspath;
    }

    @Input
    public abstract Property<String> getPackageName();

    @Input
    public abstract Property<String> getClassName();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() {
        List<File> classpathFiles = new ArrayList<>();
        for (File file : minecraftClasspath.getFiles()) {
            if (file.isFile()) {
                classpathFiles.add(file);
            }
        }
        if (classpathFiles.isEmpty()) {
            throw new GradleException(
                "Minecraft classpath is empty. Configure minecraftClasspath with the Minecraft server jar."
            );
        }

        getLogger().lifecycle("Extracting entity data metadata from Minecraft classes ({} jars)...", classpathFiles.size());
        DisplayMetadataSpec spec = DisplayMetadataGenerator.extractFromSource(classpathFiles);

        String source = DisplayMetadataGenerator.renderJavaSource(
            getPackageName().get(), getClassName().get(), spec
        );

        File outputRoot = getOutputDir().get().getAsFile();
        if (!outputRoot.exists() && !outputRoot.mkdirs()) {
            throw new GradleException("Failed to create output directory " + outputRoot + ".");
        }

        File targetFile = new File(
            new File(outputRoot, getPackageName().get().replace('.', '/')),
            getClassName().get() + ".java"
        );
        File targetParent = targetFile.getParentFile();
        if (!targetParent.exists() && !targetParent.mkdirs()) {
            throw new GradleException("Failed to create package directory " + targetParent + ".");
        }
        try {
            java.nio.file.Files.writeString(targetFile.toPath(), source, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new GradleException("Failed to write generated display metadata source.", ex);
        }

        getLogger().lifecycle(
            "Generated {} display metadata field constants at {}",
            spec.fields().size(), getProject().relativePath(targetFile)
        );
    }
}
