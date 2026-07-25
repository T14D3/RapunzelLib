package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.tasks.GenerateKeyCatalogTask;
import de.t14d3.rapunzellib.gradle.tasks.GenerateRNbtSchemaTask;
import de.t14d3.rapunzellib.gradle.tasks.InitTemplateTask;
import de.t14d3.rapunzellib.gradle.tasks.RunServersTask;
import de.t14d3.rapunzellib.gradle.tasks.ValidateMessagesTask;
import de.t14d3.rapunzellib.gradle.tasks.GenerateContextWrapperTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import java.util.List;

public final class RapunzelLibTaskRegistrars {
    private RapunzelLibTaskRegistrars() {
    }

    public static TaskProvider<GenerateKeyCatalogTask> registerKeyCatalogTask(Project project, RapunzelLibExtension extension) {
        return project.getTasks().register("rapunzellibGenerateKeyCatalog", GenerateKeyCatalogTask.class, task -> {
            task.setGroup("rapunzellib");
            task.setDescription("Generates Java key-constant catalogs from explicit namespaced-key inputs.");

            task.getInputFiles().from(extension.getKeyCatalog().getInputFiles());
            task.getPackageName().convention(extension.getKeyCatalog().getPackageName());
            task.getClassName().convention(extension.getKeyCatalog().getClassName());
            task.getDomainName().convention(extension.getKeyCatalog().getDomainName());
            task.getRegistryHelpers().convention(extension.getKeyCatalog().getRegistryHelpers());
            task.getOutputDir().convention(extension.getKeyCatalog().getOutputDir());
        });
    }

    public static TaskProvider<ValidateMessagesTask> registerValidateMessagesTask(Project project, RapunzelLibExtension extension) {
        return project.getTasks().register("rapunzellibValidateMessages", ValidateMessagesTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Validates messages.yml keys against compiled bytecode usage.");

            task.getMessagesFile().set(extension.getMessagesFile());
            task.getAdditionalMessagesFiles().set(extension.getAdditionalMessagesFiles());
            task.getFailOnUnusedKeys().set(extension.getFailOnUnusedKeys());
            task.getAlwaysUsedKeys().set(extension.getAlwaysUsedKeys());
            task.getMessageKeyCallOwners().set(extension.getMessageKeyCallOwners());
            task.getMessageKeyCallMethods().set(extension.getMessageKeyCallMethods());
            task.getMessageKeyPrefix().set(extension.getMessageKeyPrefix());
        });
    }

    public static TaskProvider<GenerateRNbtSchemaTask> registerRNbtSchemaTask(Project project, RapunzelLibExtension extension) {
        return project.getTasks().register("rapunzellibGenerateRNbtSchema", GenerateRNbtSchemaTask.class, task -> {
            task.setGroup("rapunzellib");
            task.setDescription("Generates typed Java RNbt schema/path bundles from checked-in schema inputs.");

            task.getInputFiles().from(extension.getRNbtSchema().getInputFiles());
            task.getPackageName().convention(extension.getRNbtSchema().getPackageName());
            task.getClassName().convention(extension.getRNbtSchema().getClassName());
            task.getOutputDir().convention(extension.getRNbtSchema().getOutputDir());
        });
    }

    public static TaskProvider<RunServersTask> registerRunServersTask(Project project, RunnerGradleProperties runnerProperties) {
        return project.getTasks().register("rapunzellibRunServers", RunServersTask.class, task -> {
            task.setGroup("run");
            task.setDescription("Runs Velocity + multiple Paper backends via Fill v3 (RapunzelLib runner).");

            RapunzelLibRunnerSupport.applyRunnerConventions(task, runnerProperties, false);
            task.getRegexReplaces().convention(List.of());
            task.getAdditionalArgs().convention(RapunzelLibRunnerSupport.perfAdditionalArgs(project, false));
            task.getBaseDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir("run/server-runner"));
        });
    }

    public static TaskProvider<RunServersTask> registerRunPerfServersTask(Project project, RunnerGradleProperties runnerProperties) {
        return project.getTasks().register("rapunzellibRunPerfServers", RunServersTask.class, task -> {
            task.setGroup("run");
            task.setDescription("Runs Velocity + multiple Paper backends with JFR enabled (RapunzelLib runner).");

            RapunzelLibRunnerSupport.applyRunnerConventions(task, runnerProperties, true);
            task.getRegexReplaces().convention(List.of());
            task.getAdditionalArgs().convention(RapunzelLibRunnerSupport.perfAdditionalArgs(project, true));
            task.getBaseDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir("run/server-runner"));
        });
    }

    public static void registerInitTemplateTask(Project project, RapunzelLibExtension extension) {
        project.getTasks().register("rapunzellibInitTemplate", InitTemplateTask.class, task -> {
            task.setGroup("rapunzellib");
            task.setDescription("Generates a small RapunzelLib starter template into template/.");

            task.getOutputDir().set(extension.getTemplateOutputDir());
            task.getBasePackage().set(extension.getTemplateBasePackage());
            task.getProjectName().set(extension.getTemplateProjectName());
        });
    }

    public static TaskProvider<DevRunnerTask> registerDevRunnerTask(Project project, DevRunnerExtension devRunnerExtension) {
        return project.getTasks().register("rapunzellibDevRun", DevRunnerTask.class, task -> {
            task.setGroup("run");
            task.setDescription("Runs a dev environment with configurable server topology via DevRunner.");
            task.setExtension(devRunnerExtension);
        });
    }

    public static TaskProvider<DevRunnerTask> registerDevRunnerPerfTask(Project project, DevRunnerExtension devRunnerExtension) {
        return project.getTasks().register("rapunzellibDevRunPerf", DevRunnerTask.class, task -> {
            task.setGroup("run");
            task.setDescription("Runs a dev environment with JFR profiling enabled via DevRunner.");
            task.setExtension(devRunnerExtension);
            task.getExtension().getJfrEnabled().set(true);
        });
    }

    public static TaskProvider<GenerateContextWrapperTask> registerContextWrapperTask(
        Project project, RapunzelLibExtension extension
    ) {
        // Register task lazily - no configuration action, to avoid Gradle 9.x
        // restriction on NamedDomainObjectProvider.configure during plugin resolution.
        return project.getTasks().register(
            "rapunzellibGenerateContextWrapper", GenerateContextWrapperTask.class
        );
    }

    public static void configureContextWrapperTask(
        Project project, RapunzelLibExtension extension,
        TaskProvider<GenerateContextWrapperTask> taskProvider
    ) {
        taskProvider.configure(task -> {
            task.setGroup("rapunzellib");
            task.setDescription("Generates a per-project static wrapper around RapunzelContext.");

            task.getPackageName().convention(extension.getContextWrapper().getPackageName());
            task.getClassName().convention(extension.getContextWrapper().getClassName());
            task.getOutputDir().convention(extension.getContextWrapper().getOutputDir());
        });
    }
}
