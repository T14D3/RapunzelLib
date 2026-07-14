package de.t14d3.rapunzellib.visualizer.collector;

import de.t14d3.rapunzellib.visualizer.model.Graph;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Resolves Java declarations and relationships from source and adds them to
 * the graph.
 *
 * <p>Implementations are free to use any compiler-level API (javac Trees,
 * JavaParser, Eclipse JDT). The default implementation in this module uses
 * the JDK's built-in {@code com.sun.source.*} API so no extra dependencies
 * are required.
 */
public interface SourceCollector {
    /**
     * Analyse the given Java source files and add discovered nodes and edges
     * to {@code graph}.
     *
     * <p>Only {@code sourceFiles} are compiled - auxiliary files are no longer
     * passed to javac because compiling multiple modules together causes
     * duplicate class errors when modules share packages. Cross-module symbol
     * resolution is handled via the {@code sourceRoots} ({@code -sourcepath})
     * and {@code compileClasspath} instead.
     *
     * @param graph            the graph being built
     * @param sourceSetId      id of the source set these files belong to (used as the parent for {@code contains} edges)
     * @param moduleName       the Gradle module name these files belong to
     * @param sourceFiles      the {@code .java} files to analyse and create nodes for
     * @param auxiliaryFiles   ignored (kept for backward compatibility with existing callers)
     * @param compileClasspath the compile classpath for symbol resolution (may be empty)
     * @param sourceRoots      source root directories for {@code -sourcepath} (may be empty); should include roots from all modules for cross-module resolution
     */
    void collect(Graph graph, String sourceSetId, String moduleName,
                 List<File> sourceFiles, List<File> auxiliaryFiles,
                 Collection<File> compileClasspath, Collection<File> sourceRoots);
}
