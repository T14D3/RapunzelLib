package de.t14d3.rapunzellib.visualizer.task;

import de.t14d3.rapunzellib.visualizer.VisualizerExtension;
import de.t14d3.rapunzellib.visualizer.collector.GraphBuilder;
import de.t14d3.rapunzellib.visualizer.collector.JavacSourceCollector;
import de.t14d3.rapunzellib.visualizer.model.Graph;
import de.t14d3.rapunzellib.visualizer.renderer.HtmlGenerator;
import de.t14d3.rapunzellib.visualizer.renderer.JsonWriter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * Gradle task that analyses the entire multi-module Java project and generates
 * a static, interactive HTML report.
 *
 * <p>The report is written to {@code build/reports/codebase/} by default and
 * can be opened directly in a browser without a web server.
 */
@CacheableTask
public abstract class GenerateCodebaseVisualizationTask extends DefaultTask {

    private final ConfigurableFileCollection sourceFiles = getProject().getObjects().fileCollection();

    public GenerateCodebaseVisualizationTask() {
        // The task action will produce a useful error if there are no source files.
        // With configuration-on-demand, subprojects may not be evaluated at
        // up-to-date check time, so we cannot reliably determine if sources exist here.
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getSourceFiles() {
        return sourceFiles;
    }

    @Input
    public abstract Property<Boolean> getIncludeTestSources();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() {
        boolean includeTest = getIncludeTestSources().getOrElse(false);
        GraphBuilder builder = new GraphBuilder(
            getProject().getRootProject(),
            new JavacSourceCollector(),
            includeTest
        );
        Graph graph = builder.build();

        JsonWriter jsonWriter = new JsonWriter();
        HtmlGenerator htmlGenerator = new HtmlGenerator();
        File outputDir = getOutputDir().get().getAsFile();
        htmlGenerator.generate(graph, jsonWriter, outputDir);

        getLogger().lifecycle(
            "Generated codebase visualization report at {} ({} nodes, {} edges)",
            getProject().relativePath(outputDir),
            graph.nodeCount(),
            graph.edgeCount()
        );

        if (graph.nodeCount() <= 1) {
            throw new GradleException(
                "Codebase visualization produced no nodes. Ensure the project has Java source sets " +
                "and the 'java' plugin is applied to subprojects."
            );
        }
    }
}
