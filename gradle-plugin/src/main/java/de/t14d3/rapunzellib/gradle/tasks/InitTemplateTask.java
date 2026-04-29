package de.t14d3.rapunzellib.gradle.tasks;

import de.t14d3.rapunzellib.gradle.InitTemplateRenderer;
import de.t14d3.rapunzellib.gradle.InitTemplateSpec;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

@DisableCachingByDefault
public abstract class InitTemplateTask extends DefaultTask {
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getBasePackage();

    @Input
    public abstract Property<String> getProjectName();

    @TaskAction
    public void generate() {
        File out = getOutputDir().get().getAsFile();
        out.mkdirs();

        InitTemplateSpec spec = new InitTemplateSpec(
            getBasePackage().get(),
            getProjectName().get(),
            pluginIdFor(getProjectName().get())
        );

        for (var generatedFile : InitTemplateRenderer.render(spec)) {
            write(new File(out, generatedFile.relativePath()), generatedFile.content());
        }
    }

    private String pluginIdFor(String name) {
        String idBase = NON_ALNUM.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("-").replaceAll("^-+|-+$", "");
        return idBase.isBlank() ? "rapunzellib-starter" : idBase;
    }

    private void write(File file, String content) {
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            try {
                java.nio.file.Files.writeString(file.toPath(), content);
            } catch (Exception ex) {
                throw new GradleException("Failed to write template file " + file, ex);
            }
        }
    }
}
