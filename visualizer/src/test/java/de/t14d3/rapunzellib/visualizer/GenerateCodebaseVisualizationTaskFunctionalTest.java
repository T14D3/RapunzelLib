package de.t14d3.rapunzellib.visualizer;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateCodebaseVisualizationTaskFunctionalTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesReportForMultiModuleProject() throws Exception {
        writeProject(tempDir);

        BuildResult result = VisualizerTestSupport.runGradle(tempDir, "generateCodebaseVisualization");

        assertEquals(SUCCESS, result.task(":generateCodebaseVisualization").getOutcome());

        Path reportDir = tempDir.resolve("build/reports/codebase");
        assertTrue(Files.exists(reportDir.resolve("index.html")));
        assertTrue(Files.exists(reportDir.resolve("graph.json")));
        assertTrue(Files.exists(reportDir.resolve("app.js")));
        assertTrue(Files.exists(reportDir.resolve("style.css")));
        assertTrue(Files.exists(reportDir.resolve("graph-data.js")));

        String json = Files.readString(reportDir.resolve("graph.json"));
        assertTrue(json.contains("project"), "graph.json should contain a project node");
        assertTrue(json.contains("module"), "graph.json should contain module nodes");
        assertTrue(json.contains("class"), "graph.json should contain class nodes");
        assertTrue(json.contains("extends"),
            "graph.json should contain extends edges");

        String html = Files.readString(reportDir.resolve("index.html"));
        assertTrue(html.contains("graph-data.js"), "index.html should reference graph-data.js");
        assertTrue(html.contains("app.js"), "index.html should reference app.js");
    }

    private void writeProject(Path base) {
        VisualizerTestSupport.writeFile(base, "settings.gradle",
            "rootProject.name = 'test-project'\ninclude 'mod-a', 'mod-b'");

        VisualizerTestSupport.writeFile(base, "build.gradle",
            "plugins {\n" +
            "    id 'de.t14d3.rapunzellib.visualizer'\n" +
            "}\n");

        // mod-a: a simple class
        VisualizerTestSupport.writeFile(base, "mod-a/build.gradle",
            "plugins { id 'java' }\n");
        VisualizerTestSupport.writeFile(base, "mod-a/src/main/java/com/example/a/ClassA.java",
            "package com.example.a;\n" +
            "\n" +
            "public class ClassA {\n" +
            "    public void hello() {\n" +
            "        System.out.println(\"hello\");\n" +
            "    }\n" +
            "}\n");

        // mod-b: extends ClassA, calls hello()
        VisualizerTestSupport.writeFile(base, "mod-b/build.gradle",
            "plugins { id 'java' }\n" +
            "dependencies {\n" +
            "    implementation project(':mod-a')\n" +
            "}\n");
        VisualizerTestSupport.writeFile(base, "mod-b/src/main/java/com/example/b/ClassB.java",
            "package com.example.b;\n" +
            "\n" +
            "import com.example.a.ClassA;\n" +
            "\n" +
            "public class ClassB extends ClassA {\n" +
            "    private String name;\n" +
            "\n" +
            "    public void greet() {\n" +
            "        hello();\n" +
            "    }\n" +
            "}\n");
    }
}
