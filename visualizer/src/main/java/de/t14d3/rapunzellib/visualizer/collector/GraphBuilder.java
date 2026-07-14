package de.t14d3.rapunzellib.visualizer.collector;

import de.t14d3.rapunzellib.visualizer.model.Graph;
import de.t14d3.rapunzellib.visualizer.model.Node;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates the {@link GradleModelCollector} and {@link SourceCollector}
 * to produce a complete {@link Graph}.
 *
 * <p>The Gradle collector runs first, populating the structural skeleton and
 * recording source files in the graph's transient side-channel. The source
 * collector then runs once per source set, filling in packages, types,
 * members, and their relationships.
 *
 * <p>For cross-module symbol resolution, all source roots from all source sets
 * are passed via the {@code -sourcepath} javac option. This allows javac to
 * resolve types from other modules without compiling their source files,
 * avoiding duplicate class errors when two modules share the same package.
 */
public final class GraphBuilder {
    private final Project rootProject;
    private final SourceCollector sourceCollector;
    private final boolean includeTestSources;

    public GraphBuilder(Project rootProject, SourceCollector sourceCollector, boolean includeTestSources) {
        this.rootProject = rootProject;
        this.sourceCollector = sourceCollector;
        this.includeTestSources = includeTestSources;
    }

    public Graph build() {
        Graph graph = new Graph();
        new GradleModelCollector(rootProject, includeTestSources).collect(graph);

        // Collect all source roots across all source sets for cross-module symbol resolution.
        // We do NOT collect or pass auxiliary source files to javac - compiling multiple
        // modules' sources together causes duplicate class errors when modules share packages
        // (e.g. :commands and :commands-shared both define de.t14d3.rapunzellib.commands.*).
        // Instead, we pass all source roots via -sourcepath so javac can resolve types
        // without compiling the sources.
        Set<File> allSourceRoots = new LinkedHashSet<>();
        List<File> allSourceFiles = new ArrayList<>();
        collectAllSources(allSourceFiles, allSourceRoots);

        for (Map.Entry<String, List<File>> entry : graph.allSourceFiles().entrySet()) {
            String ssId = entry.getKey();
            List<File> files = entry.getValue();
            if (files == null || files.isEmpty()) {
                continue;
            }
            Node ssNode = graph.getNode(ssId);
            String moduleName = ssNode != null ? ssNode.getContainingModule() : "";
            Collection<File> classpath = graph.getCompileClasspath(ssId);

            // No auxiliary files - cross-module resolution is handled via sourcepath.
            sourceCollector.collect(graph, ssId, moduleName, files, List.of(), classpath, allSourceRoots);
        }
        return graph;
    }

    private void collectAllSources(List<File> allFiles, Set<File> allRoots) {
        collectFromProject(rootProject, allFiles, allRoots);
        for (Project sub : rootProject.getSubprojects()) {
            collectFromProject(sub, allFiles, allRoots);
        }
    }

    private void collectFromProject(Project project, List<File> allFiles, Set<File> allRoots) {
        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets == null) {
            // Try by name (handles classloader edge cases with included builds)
            Object ext = project.getExtensions().findByName("sourceSets");
            if (ext instanceof SourceSetContainer) {
                sourceSets = (SourceSetContainer) ext;
            } else {
                return;
            }
        }
        for (SourceSet ss : sourceSets) {
            if (!includeTestSources && SourceSet.TEST_SOURCE_SET_NAME.equals(ss.getName())) continue;

            // Source roots from the source set's declared directories.
            for (File dir : ss.getJava().getSourceDirectories()) {
                if (dir.exists()) {
                    allRoots.add(dir);
                }
            }

            org.gradle.api.tasks.compile.JavaCompile compileTask =
                (org.gradle.api.tasks.compile.JavaCompile) project.getTasks().findByName(ss.getCompileJavaTaskName());
            if (compileTask != null && !compileTask.getSource().isEmpty()) {
                // Use the compile task's configured source, which may include preprocessed/generated sources.
                for (File f : compileTask.getSource()) {
                    if (f.isFile() && f.getName().endsWith(".java") && !allFiles.contains(f)) {
                        allFiles.add(f);
                    }
                }
            } else {
                // Fallback: use sourceSet.getAllJava() if compile task source is empty.
                for (File f : ss.getAllJava()) {
                    if (f.isFile() && f.getName().endsWith(".java") && !allFiles.contains(f)) {
                        allFiles.add(f);
                    }
                }
            }
        }
    }

    /**
     * Returns the set of canonical paths for the given files.
     * Canonical paths resolve symlinks and normalize {@code ..} and {@code .} segments,
     * providing a reliable key for deduplication across different path representations.
     */
    private static Set<String> canonicalPathSet(List<File> files) {
        Set<String> result = new HashSet<>();
        for (File f : files) {
            try {
                result.add(f.getCanonicalPath());
            } catch (IOException e) {
                result.add(f.getAbsolutePath());
            }
        }
        return result;
    }

    /**
     * Checks whether the given file's canonical path is in the provided set of canonical paths.
     */
    private static boolean isInCanonicalSet(File f, Set<String> canonicalSet) {
        try {
            return canonicalSet.contains(f.getCanonicalPath());
        } catch (IOException e) {
            return canonicalSet.contains(f.getAbsolutePath());
        }
    }
}
