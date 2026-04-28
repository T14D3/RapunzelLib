package de.t14d3.rapunzellib.multiversion;

import de.t14d3.rapunzellib.multiversion.tasks.PreprocessSourcesTask;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class PreprocessSourcesTaskTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
    }

    @Test
    void testPreprocessSimpleIfDirective(@TempDir File tempDir) throws IOException {
        // Setup source file
        File sourceDir = new File(tempDir, "src/main/java");
        sourceDir.mkdirs();
        File sourceFile = new File(sourceDir, "Test.java");
        String sourceContent = """
                public class Test {
                    // #if VERSION >= 1.21
                    public void modernMethod() {
                        System.out.println("Modern");
                    }
                    // #endif
                    
                    // #if VERSION < 1.21
                    public void legacyMethod() {
                        System.out.println("Legacy");
                    }
                    // #endif
                }
                """;
        Files.writeString(sourceFile.toPath(), sourceContent, StandardCharsets.UTF_8);

        // Setup output directory
        File outputDir = new File(tempDir, "generated");

        // Create and configure task
        project.getTasks().register("testPreprocess", PreprocessSourcesTask.class)
                .configure(t -> {
                    t.getSourceDir().set(sourceDir);
                    t.getOutputDir().set(outputDir);
                    t.getTargetVersion().set("1.21");
                });

        PreprocessSourcesTask task = project.getTasks().named("testPreprocess", PreprocessSourcesTask.class).get();

        // Execute task
        task.preprocess();

        // Verify output
        File outputFile = new File(outputDir, "Test.java");
        assertTrue(outputFile.exists(), "Output file should exist");

        String processedContent = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        System.out.println("Processed content:");
        System.out.println(processedContent);

        // Check that modern method is included and legacy method is commented out
        assertTrue(processedContent.contains("public void modernMethod()"), "Modern method should be included");
        assertTrue(processedContent.contains("//$$ // #if VERSION < 1.21"), "Legacy #if should be commented");
        assertTrue(processedContent.contains("//$$ // #endif"), "Legacy #endif should be commented");
        assertTrue(processedContent.contains("//$$ public void legacyMethod()"), "Legacy method should be commented out");
    }

    @Test
    void testPreprocessElseifDirective(@TempDir File tempDir) throws IOException {
        // Setup source file
        File sourceDir = new File(tempDir, "src/main/java");
        sourceDir.mkdirs();
        File sourceFile = new File(sourceDir, "Test.java");
        String sourceContent = """
                public class Test {
                    // #if VERSION >= 1.21.3
                    public void latestMethod() {
                        System.out.println("Latest");
                    }
                    // #elseif VERSION >= 1.21
                    public void modernMethod() {
                        System.out.println("Modern");
                    }
                    // #else
                    public void legacyMethod() {
                        System.out.println("Legacy");
                    }
                    // #endif
                }
                """;
        Files.writeString(sourceFile.toPath(), sourceContent, StandardCharsets.UTF_8);

        // Setup output directory
        File outputDir = new File(tempDir, "generated");

        // Create and configure task for version 1.21
        project.getTasks().register("testPreprocess", PreprocessSourcesTask.class)
                .configure(t -> {
                    t.getSourceDir().set(sourceDir);
                    t.getOutputDir().set(outputDir);
                    t.getTargetVersion().set("1.21");
                });

        PreprocessSourcesTask task = project.getTasks().named("testPreprocess", PreprocessSourcesTask.class).get();

        // Execute task
        task.preprocess();

        // Verify output
        File outputFile = new File(outputDir, "Test.java");
        assertTrue(outputFile.exists(), "Output file should exist");

        String processedContent = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        System.out.println("Processed content for 1.21:");
        System.out.println(processedContent);

        // Check that modern method is included, latest is commented out, legacy is commented out
        assertTrue(processedContent.contains("public void modernMethod()"), "Modern method should be included");
        assertTrue(processedContent.contains("//$$ // #if VERSION >= 1.21.3"), "Latest #if should be commented");
        assertTrue(processedContent.contains("//$$ // #else"), "Else should be commented");
        assertTrue(processedContent.contains("//$$ public void legacyMethod()"), "Legacy method should be commented out");
        assertFalse(
                processedContent.lines().anyMatch(line -> line.trim().equals("public void latestMethod() {")),
                "Latest method should not be included"
        );
    }

    @Test
    void testPreprocessNestedDirectives(@TempDir File tempDir) throws IOException {
        // Setup source file
        File sourceDir = new File(tempDir, "src/main/java");
        sourceDir.mkdirs();
        File sourceFile = new File(sourceDir, "Test.java");
        String sourceContent = """
                public class Test {
                    // #if VERSION >= 1.21
                        // #if VERSION >= 1.21.3
                        public void latestMethod() {
                            System.out.println("Latest");
                        }
                        // #else
                        public void modernMethod() {
                            System.out.println("Modern");
                        }
                        // #endif
                    // #else
                    public void legacyMethod() {
                        System.out.println("Legacy");
                    }
                    // #endif
                }
                """;
        Files.writeString(sourceFile.toPath(), sourceContent, StandardCharsets.UTF_8);

        // Setup output directory
        File outputDir = new File(tempDir, "generated");

        // Create and configure task for version 1.21.3
        project.getTasks().register("testPreprocess", PreprocessSourcesTask.class)
                .configure(t -> {
                    t.getSourceDir().set(sourceDir);
                    t.getOutputDir().set(outputDir);
                    t.getTargetVersion().set("1.21.3");
                });

        PreprocessSourcesTask task = project.getTasks().named("testPreprocess", PreprocessSourcesTask.class).get();

        // Execute task
        task.preprocess();

        // Verify output
        File outputFile = new File(outputDir, "Test.java");
        assertTrue(outputFile.exists(), "Output file should exist");

        String processedContent = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        System.out.println("Processed content for 1.21.3:");
        System.out.println(processedContent);

        // Check that latest method is included, modern is commented out, legacy is commented out
        assertTrue(processedContent.contains("public void latestMethod()"), "Latest method should be included");
        assertTrue(processedContent.contains("//$$ // #else"), "Inner #else should be commented");
        assertTrue(processedContent.contains("//$$ // #if VERSION >= 1.21"), "Outer #if should be commented");
        assertTrue(processedContent.contains("//$$ // #else"), "Outer #else should be commented");
        assertTrue(processedContent.contains("//$$ public void legacyMethod()"), "Legacy method should be commented out");
        assertFalse(
                processedContent.lines().anyMatch(line -> line.trim().equals("public void modernMethod() {")),
                "Modern method should not be included"
        );
    }

    @Test
    void testPreprocessKotlinFile(@TempDir File tempDir) throws IOException {
        // Setup source file
        File sourceDir = new File(tempDir, "src/main/java");
        sourceDir.mkdirs();
        File sourceFile = new File(sourceDir, "Test.kt");
        String sourceContent = """
                package com.example
                
                class Test {
                    // #if VERSION >= 1.21
                    fun modernMethod() {
                        println("Modern")
                    }
                    // #endif
                    
                    // #if VERSION < 1.21
                    fun legacyMethod() {
                        println("Legacy")
                    }
                    // #endif
                }
                """;
        Files.writeString(sourceFile.toPath(), sourceContent, StandardCharsets.UTF_8);

        // Setup output directory
        File outputDir = new File(tempDir, "generated");

        // Create and configure task
        project.getTasks().register("testPreprocess", PreprocessSourcesTask.class)
                .configure(t -> {
                    t.getSourceDir().set(sourceDir);
                    t.getOutputDir().set(outputDir);
                    t.getTargetVersion().set("1.21");
                });

        PreprocessSourcesTask task = project.getTasks().named("testPreprocess", PreprocessSourcesTask.class).get();

        // Execute task
        task.preprocess();

        // Verify output
        File outputFile = new File(outputDir, "Test.kt");
        assertTrue(outputFile.exists(), "Output file should exist");

        String processedContent = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        System.out.println("Processed Kotlin content:");
        System.out.println(processedContent);

        // Check that modern method is included and legacy method is commented out
        assertTrue(processedContent.contains("fun modernMethod()"), "Modern method should be included");
        assertTrue(processedContent.contains("//$$ // #if VERSION < 1.21"), "Legacy #if should be commented");
        assertTrue(processedContent.contains("//$$ // #endif"), "Legacy #endif should be commented");
        assertTrue(processedContent.contains("//$$ fun legacyMethod()"), "Legacy method should be commented out");
    }

    @Test
    void testPreprocessCommentedVersionBlocks(@TempDir File tempDir) throws IOException {
        File sourceDir = new File(tempDir, "src/main/java");
        sourceDir.mkdirs();
        File sourceFile = new File(sourceDir, "Test.java");
        String sourceContent = """
                public class Test {
                    public void render() {
                        // #if VERSION >= 1.21
                        // # int value = 10;
                        // # System.out.println(value);
                        // #else
                        // # int value = 20;
                        // # System.out.println(value);
                        // #endif
                    }
                }
                """;
        Files.writeString(sourceFile.toPath(), sourceContent, StandardCharsets.UTF_8);

        File outputDir = new File(tempDir, "generated");

        project.getTasks().register("testPreprocess", PreprocessSourcesTask.class)
                .configure(t -> {
                    t.getSourceDir().set(sourceDir);
                    t.getOutputDir().set(outputDir);
                    t.getTargetVersion().set("1.21");
                });

        PreprocessSourcesTask task = project.getTasks().named("testPreprocess", PreprocessSourcesTask.class).get();
        task.preprocess();

        File outputFile = new File(outputDir, "Test.java");
        assertTrue(outputFile.exists(), "Output file should exist");

        String processedContent = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);

        assertTrue(processedContent.contains("int value = 10;"), "Active commented code should be uncommented");
        assertTrue(processedContent.contains("System.out.println(value);"), "Active commented code should be uncommented");
        assertTrue(processedContent.contains("//$$ // # int value = 20;"), "Inactive commented code should stay commented");
        assertTrue(processedContent.contains("//$$ // #endif"), "Directive lines should remain commented");
        assertFalse(
                processedContent.lines().anyMatch(line -> line.trim().equals("int value = 20;")),
                "Inactive branch should not be emitted as code"
        );
    }
}
