package de.t14d3.rapunzellib.gradle.nbt;

import de.t14d3.rapunzellib.gradle.testutil.TestSupport;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateRNbtSchemaTaskFunctionalTest {
    @TempDir
    Path tempDir;

    @Test
    void generationModeEmitsTypedSchemaBundlesAndCompilesConsumerUsage() throws Exception {
        writeConsumerProject(
            """
            plugins {
                id 'java'
                id 'de.t14d3.rapunzellib'
            }

            rapunzellib {
                rNbtSchema {
                    packageName.set('com.example.nbt')
                    className.set('GeneratedEntityNbt')
                }
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/rapunzellib/rnbt-schema.yml",
            """
            name: mojang_entity_root
            entries:
              - key: Pos
                type: list
                elementType: double
              - key: WorldUUIDMost
                type: long
            """
        );
        writeStubRnbtModel();
        TestSupport.writeFile(
            tempDir,
            "src/main/java/com/example/UsesGeneratedEntityNbt.java",
            """
            package com.example;

            import com.example.nbt.GeneratedEntityNbt;
            import de.t14d3.rapunzellib.nbt.RNbtField;
            import de.t14d3.rapunzellib.nbt.RNbtPath;
            import java.util.List;

            public final class UsesGeneratedEntityNbt {
                private UsesGeneratedEntityNbt() {
                }

                public static RNbtField<List<Double>> positionField() {
                    return GeneratedEntityNbt.Fields.POS;
                }

                public static RNbtPath<Long> worldUuidMostPath() {
                    return GeneratedEntityNbt.Paths.WORLD_UUID_MOST;
                }

                public static String schemaName() {
                    return GeneratedEntityNbt.NAME;
                }
            }
            """
        );

        BuildResult result = TestSupport.runGradle(tempDir, "rapunzellibGenerateRNbtSchema", "compileJava");

        assertEquals(SUCCESS, result.task(":rapunzellibGenerateRNbtSchema").getOutcome());
        assertEquals(SUCCESS, result.task(":compileJava").getOutcome());

        Path generatedSource = generatedSource("com/example/nbt/GeneratedEntityNbt.java");
        assertTrue(Files.exists(generatedSource));
        String generated = Files.readString(generatedSource);
        assertTrue(generated.contains("public static final RNbtField<List<Double>> POS"));
        assertTrue(generated.contains("public static final RNbtPath<Long> WORLD_UUID_MOST"));
        assertTrue(Files.exists(compiledClass("com/example/UsesGeneratedEntityNbt.class")));
    }

    @Test
    void multipleSchemaTasksCanShareOneGeneratedSourceDirectory() throws Exception {
        writeConsumerProject(
            """
            import de.t14d3.rapunzellib.gradle.tasks.GenerateRNbtSchemaTask

            plugins {
                id 'java'
                id 'de.t14d3.rapunzellib'
            }

            def generatedDir = layout.projectDirectory.dir('src/generated/java')

            tasks.register('generateEntitySchema', GenerateRNbtSchemaTask) {
                inputFiles.from(layout.projectDirectory.file('src/main/rapunzellib/entity-schema.yml'))
                packageName.set('com.example.generated')
                className.set('EntityNbt')
                outputDir.set(generatedDir)
            }

            tasks.register('generateWorldSchema', GenerateRNbtSchemaTask) {
                inputFiles.from(layout.projectDirectory.file('src/main/rapunzellib/world-schema.yml'))
                packageName.set('com.example.generated')
                className.set('WorldNbt')
                outputDir.set(generatedDir)
            }

            sourceSets {
                main {
                    java.srcDir('src/generated/java')
                }
            }

            tasks.named('compileJava') {
                dependsOn(tasks.named('generateEntitySchema'))
                dependsOn(tasks.named('generateWorldSchema'))
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/rapunzellib/entity-schema.yml",
            """
            name: entity
            entries:
              - key: Pos
                type: list
                elementType: double
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/rapunzellib/world-schema.yml",
            """
            name: world
            entries:
              - key: WorldUUIDMost
                type: long
            """
        );
        writeStubRnbtModel();
        TestSupport.writeFile(
            tempDir,
            "src/main/java/com/example/UsesGeneratedSchemas.java",
            """
            package com.example;

            import com.example.generated.EntityNbt;
            import com.example.generated.WorldNbt;
            import de.t14d3.rapunzellib.nbt.RNbtField;
            import de.t14d3.rapunzellib.nbt.RNbtPath;
            import java.util.List;

            public final class UsesGeneratedSchemas {
                private UsesGeneratedSchemas() {
                }

                public static RNbtField<List<Double>> positionField() {
                    return EntityNbt.Fields.POS;
                }

                public static RNbtPath<Long> worldUuidMostPath() {
                    return WorldNbt.Paths.WORLD_UUID_MOST;
                }
            }
            """
        );

        BuildResult result = TestSupport.runGradle(tempDir, "generateEntitySchema", "generateWorldSchema", "compileJava");

        assertEquals(SUCCESS, result.task(":generateEntitySchema").getOutcome());
        assertEquals(SUCCESS, result.task(":generateWorldSchema").getOutcome());
        assertEquals(SUCCESS, result.task(":compileJava").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("src/generated/java/com/example/generated/EntityNbt.java")));
        assertTrue(Files.exists(tempDir.resolve("src/generated/java/com/example/generated/WorldNbt.java")));
        assertTrue(Files.exists(compiledClass("com/example/UsesGeneratedSchemas.class")));
    }

    private void writeConsumerProject(String buildFile) {
        TestSupport.writeFile(tempDir, "settings.gradle", "rootProject.name = 'consumer-rnbt-schema'");
        TestSupport.writeFile(tempDir, "build.gradle", buildFile.strip());
    }

    private void writeStubRnbtModel() {
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/nbt/RNbtCodec.java",
            """
            package de.t14d3.rapunzellib.nbt;

            public interface RNbtCodec<T> {
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/nbt/RNbtField.java",
            """
            package de.t14d3.rapunzellib.nbt;

            public final class RNbtField<T> {
                private final String key;
                private final RNbtPath<T> path;

                private RNbtField(String key, RNbtCodec<T> codec) {
                    this.key = key;
                    this.path = RNbtPath.of(codec, key);
                }

                public static <T> RNbtField<T> of(String key, RNbtCodec<T> codec) {
                    return new RNbtField<>(key, codec);
                }

                public RNbtPath<T> path() {
                    return path;
                }

                public String key() {
                    return key;
                }
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/nbt/RNbtPath.java",
            """
            package de.t14d3.rapunzellib.nbt;

            public final class RNbtPath<T> {
                private RNbtPath() {
                }

                public static <T> RNbtPath<T> of(RNbtCodec<T> codec, String first) {
                    return new RNbtPath<>();
                }

                public RNbtPath<T> key(String key) {
                    return this;
                }
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/nbt/RNbtSchema.java",
            """
            package de.t14d3.rapunzellib.nbt;

            public final class RNbtSchema {
                private RNbtSchema() {
                }

                public static RNbtSchema of(String name, RNbtField<?>... fields) {
                    return new RNbtSchema();
                }
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/nbt/RNbtCodecs.java",
            """
            package de.t14d3.rapunzellib.nbt;

            import java.util.List;

            public final class RNbtCodecs {
                public static final RNbtCodec<Double> DOUBLE = new SimpleCodec<>();
                public static final RNbtCodec<Long> LONG = new SimpleCodec<>();

                private RNbtCodecs() {
                }

                public static <T> RNbtCodec<List<T>> listOf(RNbtCodec<T> elementCodec) {
                    return new SimpleCodec<>();
                }

                private static final class SimpleCodec<T> implements RNbtCodec<T> {
                }
            }
            """
        );
    }

    private Path generatedSource(String relativePath) {
        return tempDir.resolve("src/generated/java/" + relativePath);
    }

    private Path compiledClass(String relativePath) {
        return tempDir.resolve("build/classes/java/main/" + relativePath);
    }
}
