package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.testutil.TestSupport;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratePlatformAdapterScaffoldTaskFunctionalTest {
    @TempDir
    Path tempDir;

    @Test
    void fabricScaffoldDefaultsToMojangSharedCoreShims() throws Exception {
        writeProject(
            """
            rapunzellib {
                scaffoldPlatformKey.set('fabric')
                scaffoldFeatures.set(['events', 'gui', 'inventory', 'nbt'])
                scaffoldOutputDir.set(layout.projectDirectory.dir('generated-scaffold'))
            }
            """
        );

        BuildResult result = TestSupport.runGradle(tempDir, "rapunzellibGeneratePlatformAdapterScaffold");

        assertEquals(SUCCESS, result.task(":rapunzellibGeneratePlatformAdapterScaffold").getOutcome());
        String buildSnippet = Files.readString(tempDir.resolve("generated-scaffold/snippets/build.gradle.kts"));

        assertTrue(buildSnippet.contains("api(project(\":platform-shared\"))"));
        assertTrue(buildSnippet.contains("implementation(project(\":events-shared\"))"));
        assertTrue(buildSnippet.contains("implementation(project(\":gui-shared\"))"));
        assertTrue(buildSnippet.contains("implementation(project(\":inventory-shared\"))"));
        assertTrue(buildSnippet.contains("implementation(project(\":nbt-shared\"))"));
        assertTrue(buildSnippet.contains("implementation(project(\":nbt-fabric\"))"));
    }

    @Test
    void paperScaffoldAutoModeKeepsSharedCoreDefaultsNarrow() throws Exception {
        writeProject(
            """
            rapunzellib {
                scaffoldPlatformKey.set('paper')
                scaffoldFeatures.set(['events', 'nbt'])
                scaffoldOutputDir.set(layout.projectDirectory.dir('generated-scaffold'))
            }
            """
        );

        BuildResult result = TestSupport.runGradle(tempDir, "rapunzellibGeneratePlatformAdapterScaffold");

        assertEquals(SUCCESS, result.task(":rapunzellibGeneratePlatformAdapterScaffold").getOutcome());
        String buildSnippet = Files.readString(tempDir.resolve("generated-scaffold/snippets/build.gradle.kts"));
        String eventsInstaller = Files.readString(tempDir.resolve(
            "generated-scaffold/events-paper/src/main/java/de/example/demo/events/paper/PaperGameEventBridgeInstaller.java"
        ));
        String nbtInstaller = Files.readString(tempDir.resolve(
            "generated-scaffold/nbt-paper/src/main/java/de/example/demo/nbt/paper/PaperNbtFeatureInstaller.java"
        ));

        assertTrue(buildSnippet.contains("api(project(\":platform-shared\"))"));
        assertTrue(buildSnippet.contains("implementation(project(\":nbt-shared\"))"));
        assertFalse(buildSnippet.contains("events-shared"));
        assertTrue(eventsInstaller.contains("TODO: hook platform-paper events into bus for this platform."));
        assertTrue(nbtInstaller.contains("keep this shim thin"));
    }

    private void writeProject(String buildFile) {
        TestSupport.writeFile(tempDir, "settings.gradle", "rootProject.name = 'scaffold-consumer'");
        TestSupport.writeFile(
            tempDir,
            "build.gradle",
            """
            plugins {
                id 'java'
                id 'de.t14d3.rapunzellib'
            }

            rapunzellib {
                scaffoldBasePackage.set('de.example.demo')
            }

            %s
            """.formatted(buildFile.strip())
        );
    }
}
