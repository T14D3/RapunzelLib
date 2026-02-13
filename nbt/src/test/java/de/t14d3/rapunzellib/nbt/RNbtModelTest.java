package de.t14d3.rapunzellib.nbt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RNbtModelTest {
    @Test
    void buildersCreateImmutableTreeValues() {
        RNbtCompound compound = RNbtCompound.builder()
            .putString("id", "minecraft:stone")
            .putBoolean("powered", true)
            .put("position", RNbtList.builder(RNbtType.INT).addInt(1).addInt(2).addInt(3).build())
            .build();

        assertEquals("minecraft:stone", RNbtCodecs.STRING.decode(compound.get("id").orElseThrow()));
        assertTrue(RNbtCodecs.BOOLEAN.decode(compound.get("powered").orElseThrow()));
        assertEquals(RNbtType.INT, compound.get("position").orElseThrow().asList().elementType());
        assertEquals(3, compound.get("position").orElseThrow().asList().size());
    }

    @Test
    void pathsTraverseNestedCompoundsAndLists() {
        RNbtPath<String> idPath = RNbtPath.of(RNbtCodecs.STRING)
            .key("inventory")
            .index(0)
            .key("id");
        RNbtPath<Integer> countPath = RNbtPath.of(RNbtCodecs.INT)
            .key("inventory")
            .index(0)
            .key("count");

        RNbtCompound root = idPath.write(RNbtCompound.empty(), "minecraft:paper");
        root = countPath.write(root, Integer.valueOf(5));

        assertEquals("minecraft:paper", idPath.read(root).orElseThrow());
        assertEquals(5, countPath.read(root).orElseThrow());

        RNbtCompound cleared = idPath.remove(root);
        assertFalse(idPath.read(cleared).isPresent());
        assertEquals(5, countPath.read(cleared).orElseThrow());
    }

    @Test
    void fieldsAndSchemaSupportTypedCompoundAccess() {
        RNbtField<Integer> health = RNbtField.of("Health", RNbtCodecs.INT);
        RNbtField<List<String>> tags = RNbtField.of("Tags", RNbtCodecs.STRING_LIST);
        RNbtSchema schema = RNbtSchema.of("entity", health, tags);

        RNbtCompound compound = tags.write(health.write(RNbtCompound.empty(), Integer.valueOf(20)), List.of("boss", "flying"));

        assertEquals("entity", schema.name());
        assertEquals(20, health.read(compound).orElseThrow());
        assertEquals(List.of("boss", "flying"), tags.read(compound).orElseThrow());
    }
}
