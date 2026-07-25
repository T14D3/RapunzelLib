package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.tasks.GenerateKeyCatalogTask;
import de.t14d3.rapunzellib.gradle.tasks.GenerateRNbtSchemaTask;
import de.t14d3.rapunzellib.gradle.tasks.ValidateMessagesTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class RapunzelLibJavaWiring {
    private RapunzelLibJavaWiring() {
    }

    public static void wireJavaPlugin(
        Project project,
        TaskProvider<GenerateKeyCatalogTask> generateKeyCatalog,
        RegistryCatalogTasks registryCatalogTasks,
        TaskProvider<ValidateMessagesTask> validateMessages,
        TaskProvider<GenerateRNbtSchemaTask> generateRNbtSchema
    ) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        @SuppressWarnings("unchecked")
        Set<File> wiredDirs = (Set<File>) project.getExtensions().getExtraProperties().get(RapunzelLibRegistryCatalogSupport.WIRED_SOURCE_DIRS_KEY);
        if (wiredDirs == null) {
            wiredDirs = new HashSet<>();
            project.getExtensions().getExtraProperties().set(RapunzelLibRegistryCatalogSupport.WIRED_SOURCE_DIRS_KEY, wiredDirs);
        }

        File keyCatalogDir = generateKeyCatalog.flatMap(GenerateKeyCatalogTask::getOutputDir).get().getAsFile();
        File rNbtSchemaDir = generateRNbtSchema.flatMap(GenerateRNbtSchemaTask::getOutputDir).get().getAsFile();
        if (wiredDirs.add(keyCatalogDir)) {
            main.getJava().srcDir(keyCatalogDir);
        }
        if (wiredDirs.add(rNbtSchemaDir)) {
            main.getJava().srcDir(rNbtSchemaDir);
        }

        validateMessages.configure(task -> task.getClassesDirs().from(main.getOutput().getClassesDirs()));

        project.getTasks().named(main.getCompileJavaTaskName()).configure(task -> {
            task.dependsOn(generateKeyCatalog);
            task.dependsOn(registryCatalogTasks.generateAll());
            task.dependsOn(generateRNbtSchema);
        });

        if (project.getTasks().findByName(main.getSourcesJarTaskName()) != null) {
            project.getTasks().named(main.getSourcesJarTaskName()).configure(task -> {
                task.dependsOn(generateKeyCatalog);
                task.dependsOn(registryCatalogTasks.generateAll());
                task.dependsOn(generateRNbtSchema);
            });
        }

        project.getTasks().named("javadoc").configure(task -> {
            task.dependsOn(generateKeyCatalog);
            task.dependsOn(registryCatalogTasks.generateAll());
            task.dependsOn(generateRNbtSchema);
        });
    }
}
