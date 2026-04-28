package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.tasks.GenerateKeyCatalogTask;
import de.t14d3.rapunzellib.gradle.tasks.GenerateRNbtSchemaTask;
import de.t14d3.rapunzellib.gradle.tasks.RunServersTask;
import de.t14d3.rapunzellib.gradle.tasks.ValidateMessagesTask;
import de.t14d3.rapunzellib.multiversion.MultiVersionExtension;
import de.t14d3.rapunzellib.multiversion.tasks.PreprocessSourcesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RapunzelLibGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        RapunzelLibPlatformCompanionWiring.wire(project);
        
        RunnerGradleProperties runnerProperties = new RunnerGradleProperties(project);
        RapunzelLibExtension extension = project.getExtensions().create("rapunzellib", RapunzelLibExtension.class);
        extension.applyDefaultConventions(project);
        
        // Add multi-version support
        MultiVersionExtension multiVersionExtension = extension.getMultiVersion();
        multiVersionExtension.getTargetVersions().convention(List.of());
        multiVersionExtension.getEnabled().convention(false);
        
        project.afterEvaluate(this::configureMultiVersionTasks);

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

    private void configureMultiVersionTasks(Project project) {
        project.getLogger().lifecycle("Configuring multi-version tasks for project: " + project.getName());
        MultiVersionExtension extension = project.getExtensions()
            .getByType(RapunzelLibExtension.class)
            .getMultiVersion();

        project.getLogger().lifecycle("MultiVersionExtension: enabled=" + extension.getEnabled().getOrElse(false) + ", versions=" + extension.getTargetVersions().get());

        if (!extension.getEnabled().getOrElse(false)) {
            project.getLogger().lifecycle("Multi-version is disabled, skipping task creation.");
            return;
        }

        Set<String> versions = new LinkedHashSet<>(extension.getTargetVersions().get());
        String coreVersion = extension.getCoreVersion().getOrElse(versions.stream().findFirst().orElse(null));
        String activeVersion = project.getProviders()
            .gradleProperty("rapunzellib.minecraftTarget")
            .getOrElse(coreVersion);
        if (activeVersion != null) {
            versions.add(activeVersion);
        }
        if (versions.isEmpty()) {
            project.getLogger().lifecycle("No target versions specified, skipping task creation.");
            return;
        }

        SourceSetContainer sourceSets = project.getExtensions()
            .getByType(SourceSetContainer.class);

        if (sourceSets == null) {
            project.getLogger().lifecycle("SourceSetContainer not found, skipping task creation.");
            return;
        }

        SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        if (mainSourceSet == null) {
            project.getLogger().lifecycle("Main source set not found, skipping task creation.");
            return;
        }

        File sourceDir = project.file("src/main/java");
        if (!sourceDir.exists()) {
            sourceDir = mainSourceSet.getAllJava().getSourceDirectories()
                .getFiles()
                .stream()
                .filter(File::exists)
                .filter(f -> f.getPath().contains("src/main/java"))
                .findFirst()
                .orElse(null);
        }
        if (sourceDir == null) {
            project.getLogger().lifecycle("Java source directory not found, skipping task creation.");
            return;
        }

        project.getLogger().lifecycle("Source directory: " + sourceDir.getAbsolutePath());

        project.getLogger().lifecycle("Core version: " + coreVersion);
        project.getLogger().lifecycle("Active version: " + activeVersion);

        TaskProvider<PreprocessSourcesTask> activeTask = null;
        File activeOutputDir = null;
        for (String version : versions) {
            project.getLogger().lifecycle("Creating task for version: " + version);
            TaskProvider<PreprocessSourcesTask> task = createVersionTask(project, version, sourceDir);
            if (version.equals(activeVersion)) {
                activeTask = task;
                activeOutputDir = multiversionOutputDir(project, version);
            }
        }

        if (activeTask != null && activeOutputDir != null) {
            TaskProvider<PreprocessSourcesTask> activePreprocessTask = activeTask;
            File compileSourceDir = activeOutputDir;
            Set<File> compileSourceDirs = new LinkedHashSet<>(mainSourceSet.getJava().getSrcDirs());
            compileSourceDirs.remove(sourceDir);
            project.getTasks().named(mainSourceSet.getCompileJavaTaskName(), JavaCompile.class).configure(task -> {
                task.dependsOn(activePreprocessTask);
                task.setSource(project.files(compileSourceDir, compileSourceDirs));
            });
            if (project.getTasks().findByName(mainSourceSet.getSourcesJarTaskName()) != null) {
                project.getTasks().named(mainSourceSet.getSourcesJarTaskName()).configure(t -> t.dependsOn(activePreprocessTask));
            }
            if (project.getTasks().findByName("javadoc") != null) {
                project.getTasks().named("javadoc", Javadoc.class).configure(task -> {
                    task.dependsOn(activePreprocessTask);
                    task.setSource(project.files(compileSourceDir, compileSourceDirs));
                });
            }
        }
    }

    private TaskProvider<PreprocessSourcesTask> createVersionTask(Project project, String version, File sourceDir) {
        String versionSafe = version.replaceAll("[^A-Za-z0-9]", "_");
        String taskName = "preprocessSourcesFor" + versionSafe;

        File outputDir = multiversionOutputDir(project, version);

        return project.getTasks().register(
            taskName,
            PreprocessSourcesTask.class,
            t -> {
                t.setDescription("Preprocess sources for Minecraft " + version);
                t.setGroup("rapunzellib");
                t.getSourceDir().set(sourceDir);
                t.getOutputDir().set(outputDir);
                t.getTargetVersion().set(version);
            }
        );
    }

    private File multiversionOutputDir(Project project, String version) {
        return new File(project.getBuildDir(), "generated-sources/multiversion/" + version);
    }
}
