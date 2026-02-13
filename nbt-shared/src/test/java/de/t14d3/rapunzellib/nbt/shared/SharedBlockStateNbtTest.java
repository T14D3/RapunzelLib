package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.shared.generated.SharedBlockStateNbt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SharedBlockStateNbtTest {
    @Test
    void generatedBlockStateBundleProvidesTypedFieldAndPathAccess() {
        RNbtCompound compound = RNbtCompound.empty();
        compound = SharedBlockStateNbt.Fields.NAME.write(compound, "minecraft:oak_sign");
        compound = SharedBlockStateNbt.Fields.PROPERTIES.write(
            compound,
            RNbtCompound.builder().putString("rotation", "2").putString("waterlogged", "false").build()
        );

        assertEquals("mojang_block_state", SharedBlockStateNbt.NAME);
        assertTrue(SharedBlockStateNbt.SCHEMA.fields().contains(SharedBlockStateNbt.Fields.NAME));
        assertTrue(SharedBlockStateNbt.SCHEMA.fields().contains(SharedBlockStateNbt.Fields.PROPERTIES));
        assertEquals("minecraft:oak_sign", SharedBlockStateNbt.Fields.NAME.read(compound).orElseThrow());
        assertEquals("2", SharedBlockStateNbt.Paths.PROPERTIES.read(compound).orElseThrow().get("rotation").orElseThrow().asPrimitive().stringValue());

        RNbtCompound stripped = compound.remove(SharedBlockStateNbt.Paths.PROPERTIES);

        assertFalse(SharedBlockStateNbt.Paths.PROPERTIES.exists(stripped));
        assertEquals("minecraft:oak_sign", SharedBlockStateNbt.Paths.NAME.read(stripped).orElseThrow());
    }
}
