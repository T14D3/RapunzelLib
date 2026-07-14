package de.t14d3.rapunzellib.visualizer;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration for the codebase visualizer plugin.
 *
 * <pre>{@code
 * codebaseVisualizer {
 *     outputDir.set(layout.buildDirectory.dir("reports/codebase"))
 *     includeTestSources.set(false)
 * }
 * }</pre>
 */
public abstract class VisualizerExtension {

    public abstract DirectoryProperty getOutputDir();

    public abstract Property<Boolean> getIncludeTestSources();

    public void applyDefaultConventions(Project project) {
        getOutputDir().convention(project.getLayout().getBuildDirectory().dir("reports/codebase"));
        getIncludeTestSources().convention(false);
    }
}
