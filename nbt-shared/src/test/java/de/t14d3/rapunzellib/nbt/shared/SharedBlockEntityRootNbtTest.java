package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.generated.BlockEntityRootNbt;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockEntityRootNbtTest {
    @Test
    void generatedBlockEntityBundleProvidesTypedFieldAndPathAccess() {
        RNbtCompound compound = RNbtCompound.empty();
        compound = BlockEntityRootNbt.Fields.ID.write(compound, "minecraft:chest");
        compound = BlockEntityRootNbt.Fields.X.write(compound, 12);
        compound = BlockEntityRootNbt.Fields.Y.write(compound, 64);
        compound = BlockEntityRootNbt.Fields.Z.write(compound, -4);
        compound = BlockEntityRootNbt.Fields.ITEMS.write(
            compound,
            List.of(RNbtCompound.builder().putByte("Slot", (byte) 0).putString("id", "minecraft:diamond").putInt("count", 3).build())
        );
        compound = BlockEntityRootNbt.Fields.CUSTOM_NAME.write(compound, Component.text("Vault"));
        compound = BlockEntityRootNbt.Fields.LOCK.write(compound, "secret");
        compound = BlockEntityRootNbt.Fields.LOOT_TABLE.write(compound, "minecraft:chests/village/village_toolsmith");
        compound = BlockEntityRootNbt.Fields.LOOT_TABLE_SEED.write(compound, 42L);

        assertEquals("mojang_block_entity_root", BlockEntityRootNbt.NAME);
        assertEquals(List.of("id", "x", "y", "z"), BlockEntityRootNbt.SCHEMA.fields().stream()
            .filter(field -> field.key().equals("id") || field.key().equals("x") || field.key().equals("y") || field.key().equals("z"))
            .map(RNbtField::key)
            .toList());
        assertTrue(BlockEntityRootNbt.SCHEMA.fields().contains(BlockEntityRootNbt.Fields.ITEMS));
        assertEquals("minecraft:chest", BlockEntityRootNbt.Fields.ID.read(compound).orElseThrow());
        assertEquals(64, BlockEntityRootNbt.Fields.Y.read(compound).orElseThrow());
        assertEquals(3, BlockEntityRootNbt.Paths.ITEMS.read(compound).orElseThrow().getFirst().get("count").orElseThrow().asPrimitive().intValue());
        assertEquals(Component.text("Vault"), BlockEntityRootNbt.Paths.CUSTOM_NAME.read(compound).orElseThrow());
        assertEquals("secret", BlockEntityRootNbt.Fields.LOCK.read(compound).orElseThrow());
        assertEquals("minecraft:chests/village/village_toolsmith", BlockEntityRootNbt.Fields.LOOT_TABLE.read(compound).orElseThrow());
        assertEquals(42L, BlockEntityRootNbt.Fields.LOOT_TABLE_SEED.read(compound).orElseThrow());

        RNbtCompound stripped = compound.remove(BlockEntityRootNbt.Paths.ITEMS).remove(BlockEntityRootNbt.Paths.CUSTOM_NAME);

        assertFalse(BlockEntityRootNbt.Paths.ITEMS.exists(stripped));
        assertFalse(BlockEntityRootNbt.Paths.CUSTOM_NAME.exists(stripped));
        assertTrue(BlockEntityRootNbt.Paths.LOCK.exists(stripped));
    }
}
