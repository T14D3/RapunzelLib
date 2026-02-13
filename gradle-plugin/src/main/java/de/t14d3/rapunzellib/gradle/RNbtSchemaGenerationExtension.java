package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class RNbtSchemaGenerationExtension {
    private final ConfigurableFileCollection inputFiles;

    @Inject
    public RNbtSchemaGenerationExtension(ObjectFactory objects) {
        this.inputFiles = objects.fileCollection();
    }

    public ConfigurableFileCollection getInputFiles() {
        return inputFiles;
    }

    public abstract Property<String> getPackageName();

    public abstract Property<String> getClassName();

    public abstract DirectoryProperty getOutputDir();

    public void applyDefaultConventions(Project project) {
        getInputFiles().from(project.getLayout().getProjectDirectory().file("src/main/rapunzellib/rnbt-schema.yml"));
        getPackageName().convention("generated.rapunzellib.nbt");
        getClassName().convention("GeneratedRNbtSchema");
        getOutputDir().convention(project.getLayout().getBuildDirectory().dir("generated/sources/rapunzellib/rnbtSchema/main/java"));
    }
}
