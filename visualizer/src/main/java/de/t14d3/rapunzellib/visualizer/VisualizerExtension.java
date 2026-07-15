package de.t14d3.rapunzellib.visualizer;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration for the codebase visualizer plugin.
 *
 * <pre>{@code
 * codebaseVisualizer {
 *     outputDir.set(layout.buildDirectory.dir("reports/codebase"))
 *     includeTestSources.set(false)
 *     // Exclude source files matching these path patterns from collection.
 *     // Patterns are matched against the canonical path of each source file
 *     // using simple glob rules (single-star = any chars, double-star = any path depth).
 *     excludePaths.addAll(
 *         "&#42;&#42;/generated-sources/&#42;&#42;",
 *         "&#42;&#42;/build/generated/&#42;&#42;"
 *     )
 * }
 * }</pre>
 */
public abstract class VisualizerExtension {

    public abstract DirectoryProperty getOutputDir();

    public abstract Property<Boolean> getIncludeTestSources();

    /**
     * Glob patterns matched against the canonical path of each source file.
     * Matching files are excluded from collection entirely - they are not
     * parsed by javac and produce no nodes or edges. This is the most
     * efficient way to skip large generated sources (e.g. Minecraft block
     * type catalogs with thousands of fields).
     *
     * <p>Patterns use simple glob rules:
     * <ul>
     *   <li>{@code *} matches any characters within a single path segment</li>
     *   <li>{@code **} matches any number of path segments (including zero)</li>
     *   <li>{@code ?} matches a single character</li>
     * </ul>
     * <p>Matching is case-sensitive and against the full canonical path.
     */
    public abstract ListProperty<String> getExcludePaths();

    public void applyDefaultConventions(Project project) {
        getOutputDir().convention(project.getLayout().getBuildDirectory().dir("reports/codebase"));
        getIncludeTestSources().convention(false);
        getExcludePaths().convention(java.util.List.of());
    }
}
