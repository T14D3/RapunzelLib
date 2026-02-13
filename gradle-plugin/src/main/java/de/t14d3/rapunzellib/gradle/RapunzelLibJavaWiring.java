package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.tasks.GenerateKeyCatalogTask;
import de.t14d3.rapunzellib.gradle.tasks.GenerateRNbtSchemaTask;
import de.t14d3.rapunzellib.gradle.tasks.RunServersTask;
import de.t14d3.rapunzellib.gradle.tasks.ValidateMessagesTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import java.util.List;

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

        main.getJava().srcDir(generateKeyCatalog.flatMap(GenerateKeyCatalogTask::getOutputDir));
        main.getJava().srcDir(generateRNbtSchema.flatMap(GenerateRNbtSchemaTask::getOutputDir));

        validateMessages.configure(task -> task.getClassesDirs().from(main.getOutput().getClassesDirs()));

        project.getTasks().named(main.getCompileJavaTaskName()).configure(task -> {
            task.dependsOn(generateKeyCatalog);
            task.dependsOn(registryCatalogTasks.generateAll());
            task.dependsOn(generateRNbtSchema);
        });

        project.getTasks().named("check").configure(task -> {
            if (registryCatalogTasks.includeVerifyParityInCheck().get()) {
                task.dependsOn(registryCatalogTasks.verifyParity());
            }
        });
    }

    public static void wireArchiveTasksAfterEvaluate(
        Project project,
        TaskProvider<RunServersTask> runServersTask,
        TaskProvider<RunServersTask> runPerfServersTask
    ) {
        project.afterEvaluate(ignored -> {
            List<TaskProvider<RunServersTask>> runTasks = List.of(runServersTask, runPerfServersTask);

            AbstractArchiveTask paperJar = archiveTask(project, "paperJar");
            if (paperJar != null) {
                for (TaskProvider<RunServersTask> runTask : runTasks) {
                    runTask.configure(task -> {
                        task.dependsOn(paperJar);
                        task.getPaperPluginJar().set(paperJar.getArchiveFile());
                    });
                }
            }

            AbstractArchiveTask velocityJar = archiveTask(project, "velocityJar");
            if (velocityJar != null) {
                for (TaskProvider<RunServersTask> runTask : runTasks) {
                    runTask.configure(task -> {
                        task.dependsOn(velocityJar);
                        task.getVelocityPluginJar().set(velocityJar.getArchiveFile());
                    });
                }
            }
        });
    }

    private static AbstractArchiveTask archiveTask(Project project, String name) {
        Object task = project.getTasks().findByName(name);
        return task instanceof AbstractArchiveTask archiveTask ? archiveTask : null;
    }
}
