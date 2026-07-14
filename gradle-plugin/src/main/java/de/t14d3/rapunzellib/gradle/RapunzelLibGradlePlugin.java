package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.tasks.GenerateContextWrapperTask;
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

        // DevRunner extension and tasks
        DevRunnerExtension devRunnerExtension = project.getExtensions().create("devRunner", DevRunnerExtension.class);
        devRunnerExtension.applyConventions(project);
        RapunzelLibTaskRegistrars.registerDevRunnerTask(project, devRunnerExtension);
        RapunzelLibTaskRegistrars.registerDevRunnerPerfTask(project, devRunnerExtension);

        RapunzelLibTaskRegistrars.registerInitTemplateTask(project, extension);
        RapunzelLibTaskRegistrars.registerPlatformAdapterScaffoldTask(project, extension);
        RapunzelLibTaskRegistrars.registerInstallerWiringTask(project);
        RapunzelLibTaskRegistrars.registerSharedParityTask(project);
        // Context wrapper task - register early so it's available in afterEvaluate.
        project.afterEvaluate(p -> {
            if (!extension.getContextWrapper().getEnabled().get()) {
                return;
            }

            TaskProvider<GenerateContextWrapperTask> ctxWrapper =
                RapunzelLibTaskRegistrars.registerContextWrapperTask(p, extension);
            RapunzelLibTaskRegistrars.configureContextWrapperTask(p, extension, ctxWrapper);

            if (p.getPlugins().hasPlugin("java")) {
                SourceSet mainSourceSet = p.getExtensions().getByType(SourceSetContainer.class)
                    .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

                // Find the main source directory (e.g. src/main/java)
                File sourceDir = mainSourceSet.getAllJava().getSourceDirectories()
                    .getFiles().stream()
                    .filter(File::exists)
                    .filter(f -> f.toPath().endsWith("src/main/java"))
                    .findFirst()
                    .orElse(null);

                if (sourceDir != null && extension.getContextWrapper().getTransformSources().get()) {
                    final File sourceDirFinal = sourceDir;
                    ctxWrapper.configure(task -> task.getSourceDir().set(sourceDirFinal));

                    // Replace the original source dir with the wrapper output dir
                    // in the compile task, preserving any other source dirs.
                    // Use canonical paths to avoid path representation mismatches.
                    p.getTasks().named(mainSourceSet.getCompileJavaTaskName(), JavaCompile.class)
                        .configure(compile -> {
                            compile.dependsOn(ctxWrapper);
                            compile.setSource(p.files(p.provider(() -> {
                                Set<File> dirs = new LinkedHashSet<>(mainSourceSet.getJava().getSrcDirs());
                                String srcDirCanonical = canonicalPath(sourceDirFinal);
                                dirs.removeIf(f -> canonicalPath(f).equals(srcDirCanonical));
                                dirs.add(ctxWrapper.get().getOutputDir().get().getAsFile());
                                return dirs;
                            })));
                        });
                }
            }
        });

        project.getPlugins().withId("java", ignored -> {
            RapunzelLibJavaWiring.wireJavaPlugin(
                project,
                generateKeyCatalog,
                registryCatalogTasks,
                validateMessages,
                generateRNbtSchema
            );
        });

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

        TaskProvider<PreprocessSourcesTask> activeMainTask = createAndWirePreprocessTasks(
            project,
            versions,
            activeVersion,
            mainSourceSet,
            sourceDir,
            "preprocessSourcesFor",
            "generated-sources/multiversion"
        );

        if (activeMainTask != null) {
            if (project.getTasks().findByName(mainSourceSet.getSourcesJarTaskName()) != null) {
                project.getTasks().named(mainSourceSet.getSourcesJarTaskName()).configure(t -> t.dependsOn(activeMainTask));
            }
            if (project.getTasks().findByName("javadoc") != null) {
                File javadocSourceDir = multiversionOutputDir(project, activeVersion, "generated-sources/multiversion");
                Set<File> javadocSourceDirs = new LinkedHashSet<>(mainSourceSet.getJava().getSrcDirs());
                String srcDirCanonical = canonicalPath(sourceDir);
                javadocSourceDirs.removeIf(f -> canonicalPath(f).equals(srcDirCanonical));
                project.getTasks().named("javadoc", Javadoc.class).configure(task -> {
                    task.dependsOn(activeMainTask);
                    task.setSource(project.files(javadocSourceDir, javadocSourceDirs));
                });
            }
        }

        SourceSet testSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME);
        if (testSourceSet != null) {
            File testSourceDir = project.file("src/test/java");
            if (testSourceDir.exists()) {
                createAndWirePreprocessTasks(
                    project,
                    versions,
                    activeVersion,
                    testSourceSet,
                    testSourceDir,
                    "preprocessTestSourcesFor",
                    "generated-test-sources/multiversion"
                );
            }
        }
    }

    private TaskProvider<PreprocessSourcesTask> createAndWirePreprocessTasks(
        Project project,
        Set<String> versions,
        String activeVersion,
        SourceSet sourceSet,
        File sourceDir,
        String taskPrefix,
        String outputRoot
    ) {
        TaskProvider<PreprocessSourcesTask> activeTask = null;
        File activeOutputDir = null;
        for (String version : versions) {
            project.getLogger().lifecycle("Creating task for version: " + version);
            TaskProvider<PreprocessSourcesTask> task = createVersionTask(project, version, sourceDir, taskPrefix, outputRoot);
            if (version.equals(activeVersion)) {
                activeTask = task;
                activeOutputDir = multiversionOutputDir(project, version, outputRoot);
            }
        }

        if (activeTask != null && activeOutputDir != null) {
            TaskProvider<PreprocessSourcesTask> activePreprocessTask = activeTask;
            File compileSourceDir = activeOutputDir;
            Set<File> compileSourceDirs = new LinkedHashSet<>(sourceSet.getJava().getSrcDirs());
            // Remove the original source directory using canonical paths to handle
            // path representation differences (e.g. relative vs absolute, symlinks).
            // This prevents both the original AND preprocessed sources from appearing
            // in the compile task source, which would cause duplicate class errors.
            String sourceDirCanonical = canonicalPath(sourceDir);
            compileSourceDirs.removeIf(f -> canonicalPath(f).equals(sourceDirCanonical));
            project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class).configure(task -> {
                task.dependsOn(activePreprocessTask);
                task.setSource(project.files(compileSourceDir, compileSourceDirs));
            });
        }

        return activeTask;
    }

    private TaskProvider<PreprocessSourcesTask> createVersionTask(Project project, String version, File sourceDir) {
        return createVersionTask(project, version, sourceDir, "preprocessSourcesFor", "generated-sources/multiversion");
    }

    private TaskProvider<PreprocessSourcesTask> createVersionTask(
        Project project,
        String version,
        File sourceDir,
        String taskPrefix,
        String outputRoot
    ) {
        String versionSafe = version.replaceAll("[^A-Za-z0-9]", "_");
        String taskName = taskPrefix + versionSafe;

        File outputDir = multiversionOutputDir(project, version, outputRoot);

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
        return multiversionOutputDir(project, version, "generated-sources/multiversion");
    }

    private File multiversionOutputDir(Project project, String version, String outputRoot) {
        return new File(project.getBuildDir(), outputRoot + "/" + version);
    }

    private static String canonicalPath(File file) {
        if (file == null) return "";
        try {
            return file.getCanonicalPath();
        } catch (java.io.IOException e) {
            return file.getAbsolutePath();
        }
    }
}
