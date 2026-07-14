package de.t14d3.rapunzellib.visualizer;

import de.t14d3.rapunzellib.visualizer.task.GenerateCodebaseVisualizationTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin entry point for the codebase visualizer.
 *
 * <p>Apply this plugin to the root project of a multi-module build:
 * <pre>{@code
 * plugins {
 *     id("de.t14d3.rapunzellib.visualizer")
 * }
 * }</pre>
 *
 * <p>This registers the {@code generateCodebaseVisualization} task, which
 * analyses all subprojects with the {@code java} plugin and produces a static
 * HTML report.
 */
public final class VisualizerGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        VisualizerExtension extension = project.getExtensions().create(
            "codebaseVisualizer", VisualizerExtension.class
        );
        extension.applyDefaultConventions(project);

        TaskProvider<GenerateCodebaseVisualizationTask> task = project.getTasks().register(
            "generateCodebaseVisualization", GenerateCodebaseVisualizationTask.class, t -> {
                t.setGroup("verification");
                t.setDescription(
                    "Generates an interactive HTML report visualizing the project's codebase structure."
                );
                t.getOutputDir().convention(extension.getOutputDir());
                t.getIncludeTestSources().convention(extension.getIncludeTestSources());
            }
        );

        // Gather source files lazily so they are resolved at task execution time.
        // This ensures the compile task source (which may include preprocessor output)
        // is up to date when we read it.
        task.configure(t -> {
            t.getSourceFiles().from(project.provider(() -> {
                ConfigurableFileCollection sources = project.getObjects().fileCollection();
                gatherJavaSources(project, sources, extension.getIncludeTestSources().getOrElse(false));
                return sources.getFiles();
            }));
        });
    }

    private static void gatherJavaSources(Project root, ConfigurableFileCollection sources, boolean includeTest) {
        gatherFromProject(root, sources, includeTest);
        for (Project sub : root.getSubprojects()) {
            gatherFromProject(sub, sources, includeTest);
        }
    }

    private static void gatherFromProject(Project project, ConfigurableFileCollection sources, boolean includeTest) {
        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets == null) {
            // Fallback: try by name (handles classloader edge cases with included builds)
            Object ext = project.getExtensions().findByName("sourceSets");
            if (ext instanceof SourceSetContainer) {
                sourceSets = (SourceSetContainer) ext;
            } else {
                return;
            }
        }
        for (SourceSet ss : sourceSets) {
            if (!includeTest && SourceSet.TEST_SOURCE_SET_NAME.equals(ss.getName())) {
                continue;
            }
            // Use the JavaCompile task's configured source (which includes preprocessed/generated sources)
            // rather than ss.getAllJava() (which only returns the original declared source directories).
            org.gradle.api.tasks.compile.JavaCompile compileTask =
                (org.gradle.api.tasks.compile.JavaCompile) project.getTasks().findByName(ss.getCompileJavaTaskName());
            if (compileTask != null && !compileTask.getSource().isEmpty()) {
                sources.from(compileTask.getSource());
            } else {
                // Fallback: use sourceSet.getAllJava()
                sources.from(ss.getAllJava());
            }
        }
    }
}
