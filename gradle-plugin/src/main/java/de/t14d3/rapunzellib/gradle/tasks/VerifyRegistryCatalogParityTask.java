package de.t14d3.rapunzellib.gradle.tasks;

import de.t14d3.rapunzellib.gradle.catalog.RegistryCatalogParityVerifier;
import de.t14d3.rapunzellib.gradle.catalog.RegistryCatalogSourceDefinition;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

public abstract class VerifyRegistryCatalogParityTask extends DefaultTask {
    private final ConfigurableFileCollection parityClasspath = getProject().getObjects().fileCollection();

    public VerifyRegistryCatalogParityTask() {
        getSourceDefinitions().convention(List.of());
        onlyIf("at least two registry catalog parity sources are configured", task -> getSourceDefinitions().get().size() >= 2);
    }

    @Input
    public abstract Property<String> getCatalogName();

    @Input
    public abstract ListProperty<String> getSourceDefinitions();

    @Classpath
    public ConfigurableFileCollection getParityClasspath() {
        return parityClasspath;
    }

    @TaskAction
    public void verify() {
        var result = RegistryCatalogParityVerifier.verify(
            getCatalogName().get(),
            getSourceDefinitions().get().stream().map(RegistryCatalogSourceDefinition::decode).toList()
        );
        getLogger().lifecycle(
            "Verified {} registry catalog parity: {} keys across {} and {}",
            result.catalogName(),
            result.entryCount(),
            result.canonicalSource().name(),
            result.comparedSources().stream().map(RegistryCatalogSourceDefinition::name).collect(java.util.stream.Collectors.joining(", "))
        );
    }
}
