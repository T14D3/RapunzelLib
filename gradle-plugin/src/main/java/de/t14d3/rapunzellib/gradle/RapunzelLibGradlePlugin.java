package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.tasks.GenerateKeyCatalogTask;
import de.t14d3.rapunzellib.gradle.tasks.GenerateRNbtSchemaTask;
import de.t14d3.rapunzellib.gradle.tasks.RunServersTask;
import de.t14d3.rapunzellib.gradle.tasks.ValidateMessagesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public final class RapunzelLibGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        RapunzelLibPlatformCompanionWiring.wire(project);

        RunnerGradleProperties runnerProperties = new RunnerGradleProperties(project);
        RapunzelLibExtension extension = project.getExtensions().create("rapunzellib", RapunzelLibExtension.class);
        extension.applyDefaultConventions(project);

        TaskProvider<GenerateKeyCatalogTask> generateKeyCatalog =
            RapunzelLibTaskRegistrars.registerKeyCatalogTask(project, extension);
        RegistryCatalogTasks registryCatalogTasks =
            RapunzelLibRegistryCatalogSupport.registerRegistryCatalogTasks(project, extension);
        TaskProvider<ValidateMessagesTask> validateMessages =
            RapunzelLibTaskRegistrars.registerValidateMessagesTask(project, extension);
        TaskProvider<GenerateRNbtSchemaTask> generateRNbtSchema =
            RapunzelLibTaskRegistrars.registerRNbtSchemaTask(project, extension);
        TaskProvider<RunServersTask> runServersTask =
            RapunzelLibTaskRegistrars.registerRunServersTask(project, runnerProperties);
        TaskProvider<RunServersTask> runPerfServersTask =
            RapunzelLibTaskRegistrars.registerRunPerfServersTask(project, runnerProperties);

        RapunzelLibTaskRegistrars.registerInitTemplateTask(project, extension);
        RapunzelLibTaskRegistrars.registerPlatformAdapterScaffoldTask(project, extension);
        RapunzelLibTaskRegistrars.registerInstallerWiringTask(project);
        RapunzelLibTaskRegistrars.registerSharedParityTask(project);

        project.getPlugins().withId("java", ignored ->
            RapunzelLibJavaWiring.wireJavaPlugin(
                project,
                generateKeyCatalog,
                registryCatalogTasks,
                validateMessages,
                generateRNbtSchema
            )
        );

        RapunzelLibJavaWiring.wireArchiveTasksAfterEvaluate(project, runServersTask, runPerfServersTask);
    }
}
