package de.t14d3.rapunzellib.gradle.nbt;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RNbtSchemaGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void parseInputFilesLoadsTypedEntriesAndNestedCompounds() throws Exception {
        Path input = tempDir.resolve("entity.yml");
        Files.writeString(
            input,
            """
            name: mojang_entity_root
            entries:
              - key: Pos
                type: list
                elementType: double
              - key: Rotation
                type: list
                elementType: float
              - key: Brain
                type: compound
                children:
                  - key: memories
                    type: compound
            """
        );

        RNbtSchemaSpec schema = RNbtSchemaGenerator.parseInputFiles(List.of(input.toFile()));

        assertEquals("mojang_entity_root", schema.name());
        assertEquals(List.of("Pos", "Rotation", "Brain", "Brain.memories"), schema.flattenedEntries().stream().map(entry -> String.join(".", entry.segments())).toList());
        assertEquals("List<Double>", schema.entries().getFirst().codec().javaType());
        assertEquals("RNbtCompound", schema.entries().get(2).codec().javaType());
    }

    @Test
    void renderJavaSourceEmitsFieldAndPathBundles() {
        String source = RNbtSchemaGenerator.renderJavaSource(
            "com.example.nbt",
            "GeneratedEntityNbt",
            new RNbtSchemaSpec(
                "mojang_entity_root",
                List.of(
                    new RNbtSchemaEntrySpec("Pos", new ListCodecSpec(new ScalarCodecSpec(ScalarCodecKind.DOUBLE)), List.of()),
                    new RNbtSchemaEntrySpec("WorldUUIDMost", new ScalarCodecSpec(ScalarCodecKind.LONG), List.of()),
                    new RNbtSchemaEntrySpec(
                        "Brain",
                        new ScalarCodecSpec(ScalarCodecKind.COMPOUND),
                        List.of(new RNbtSchemaEntrySpec("memories", new ScalarCodecSpec(ScalarCodecKind.COMPOUND), List.of()))
                    )
                )
            )
        );

        assertTrue(source.contains("public static final RNbtField<List<Double>> POS"));
        assertTrue(source.contains("public static final RNbtField<Long> WORLD_UUID_MOST"));
        assertTrue(source.contains("public static final RNbtPath<List<Double>> POS = path(RNbtCodecs.listOf(RNbtCodecs.DOUBLE), \"Pos\");"));
        assertTrue(source.contains("public static final RNbtPath<RNbtCompound> BRAIN_MEMORIES = path(RNbtCodecs.COMPOUND, \"Brain\", \"memories\");"));
        assertTrue(source.contains("public static final RNbtSchema SCHEMA = RNbtSchema.of("));
    }

    @Test
    void parseInputFilesRejectsDuplicatePaths() throws Exception {
        Path input = tempDir.resolve("duplicate.yml");
        Files.writeString(
            input,
            """
            name: mojang_entity_root
            entries:
              - key: Pos
                type: list
                elementType: double
              - key: Pos
                type: list
                elementType: double
            """
        );

        GradleException failure = assertThrows(GradleException.class, () -> RNbtSchemaGenerator.parseInputFiles(List.of(input.toFile())));

        assertTrue(failure.getMessage().contains("Duplicate RNbt schema entry 'Pos'"));
    }
}
