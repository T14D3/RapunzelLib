package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.generated.BlockStateNbt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockStateNbtTest {
    @Test
    void generatedBlockStateBundleProvidesTypedFieldAndPathAccess() {
        RNbtCompound compound = RNbtCompound.empty();
        compound = BlockStateNbt.Fields.NAME.write(compound, "minecraft:oak_sign");
        compound = BlockStateNbt.Fields.PROPERTIES.write(
            compound,
            RNbtCompound.builder().putString("rotation", "2").putString("waterlogged", "false").build()
        );

        assertEquals("mojang_block_state", BlockStateNbt.NAME);
        assertTrue(BlockStateNbt.SCHEMA.fields().contains(BlockStateNbt.Fields.NAME));
        assertTrue(BlockStateNbt.SCHEMA.fields().contains(BlockStateNbt.Fields.PROPERTIES));
        assertEquals("minecraft:oak_sign", BlockStateNbt.Fields.NAME.read(compound).orElseThrow());
        assertEquals("2", BlockStateNbt.Paths.PROPERTIES.read(compound).orElseThrow().get("rotation").orElseThrow().asPrimitive().stringValue());

        RNbtCompound stripped = compound.remove(BlockStateNbt.Paths.PROPERTIES);

        assertFalse(BlockStateNbt.Paths.PROPERTIES.exists(stripped));
        assertEquals("minecraft:oak_sign", BlockStateNbt.Paths.NAME.read(stripped).orElseThrow());
    }
}
