package de.t14d3.rapunzellib.gradle;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class KeyCatalogGenerationExtension {
    private final ConfigurableFileCollection inputFiles;

    @Inject
    public KeyCatalogGenerationExtension(ObjectFactory objects) {
        this.inputFiles = objects.fileCollection();
    }

    public ConfigurableFileCollection getInputFiles() {
        return inputFiles;
    }

    public abstract Property<String> getPackageName();

    public abstract Property<String> getClassName();

    public abstract Property<String> getDomainName();

    public abstract SetProperty<String> getRegistryHelpers();

    public abstract DirectoryProperty getOutputDir();

    public void applyDefaultConventions(Project project) {
        getInputFiles().from(project.getLayout().getProjectDirectory().file("src/main/rapunzellib/keys.txt"));
        getPackageName().convention("generated.rapunzellib.keys");
        getClassName().convention("GeneratedKeys");
        getDomainName().convention(project.getName());
        getRegistryHelpers().convention(java.util.Set.of());
        getOutputDir().convention(project.getLayout().getBuildDirectory().dir("generated/sources/rapunzellib/keyCatalog/main/java"));
    }
}
