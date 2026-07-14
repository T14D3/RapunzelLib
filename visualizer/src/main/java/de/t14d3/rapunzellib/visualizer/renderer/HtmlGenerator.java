package de.t14d3.rapunzellib.visualizer.renderer;

import de.t14d3.rapunzellib.visualizer.model.Graph;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Writes the complete static report to a directory:
 *
 * <ul>
 *   <li>{@code index.html} - entry point (copied from classpath resources)</li>
 *   <li>{@code app.js} - application logic (copied from classpath resources)</li>
 *   <li>{@code style.css} - styling (copied from classpath resources)</li>
 *   <li>{@code graph.json} - neutral JSON graph (compact, for programmatic access / HTTP serving)</li>
 *   <li>{@code graph-data.js} - compact binary payload base64-embedded as a JS variable
 *       (for {@code file://} protocol; decoded by {@code app.decode.js})</li>
 * </ul>
 *
 * <p>The binary {@code graph-data.js} is the primary transport used by the
 * WebGL2 renderer. It is typically 30-50x smaller than the equivalent JSON
 * thanks to string interning and columnar edge encoding. The compact
 * {@code graph.json} is retained for tooling that expects JSON.
 */
public final class HtmlGenerator {
    private static final String RESOURCE_PREFIX = "/de/t14d3/rapunzellib/visualizer/ui/";
    private static final String[] STATIC_RESOURCES = {
        "index.html", "style.css",
        "app.js", "app.constants.js", "app.indices.js",
        "app.layout.tree.js", "app.layout.radial.js", "app.layout.cluster.js",
        "app.focus.js", "app.render.js", "app.interact.js", "app.ui.js",
        "app.decode.js", "app.webgl.js"
    };

    public void generate(Graph graph, JsonWriter jsonWriter, File outputDir) {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new GradleException("Failed to create output directory: " + outputDir);
        }

        for (String resource : STATIC_RESOURCES) {
            copyResource(RESOURCE_PREFIX + resource, new File(outputDir, resource));
        }

        // Primary transport: compact binary embedded in JS (works under file://).
        BinaryGraphWriter binaryWriter = new BinaryGraphWriter();
        writeFile(new File(outputDir, "graph-data.js"), binaryWriter.writeJsEmbed(graph));

        // Secondary transport: compact JSON for programmatic/HTTP consumers.
        String json = jsonWriter.writeJson(graph);
        writeFile(new File(outputDir, "graph.json"), json);
    }

    private void copyResource(String resourcePath, File target) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new GradleException("Visualizer resource not found: " + resourcePath);
            }
            Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new GradleException("Failed to copy visualizer resource: " + resourcePath, e);
        }
    }

    private void writeFile(File target, String content) {
        try {
            Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("Failed to write visualizer file: " + target, e);
        }
    }
}
