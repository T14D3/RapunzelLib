package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.shared.generated.SharedBlockEntityRootNbt;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SharedBlockEntityRootNbtTest {
    @Test
    void generatedBlockEntityBundleProvidesTypedFieldAndPathAccess() {
        RNbtCompound compound = RNbtCompound.empty();
        compound = SharedBlockEntityRootNbt.Fields.ID.write(compound, "minecraft:chest");
        compound = SharedBlockEntityRootNbt.Fields.X.write(compound, 12);
        compound = SharedBlockEntityRootNbt.Fields.Y.write(compound, 64);
        compound = SharedBlockEntityRootNbt.Fields.Z.write(compound, -4);
        compound = SharedBlockEntityRootNbt.Fields.ITEMS.write(
            compound,
            List.of(RNbtCompound.builder().putByte("Slot", (byte) 0).putString("id", "minecraft:diamond").putInt("count", 3).build())
        );
        compound = SharedBlockEntityRootNbt.Fields.CUSTOM_NAME.write(compound, Component.text("Vault"));
        compound = SharedBlockEntityRootNbt.Fields.LOCK.write(compound, "secret");
        compound = SharedBlockEntityRootNbt.Fields.LOOT_TABLE.write(compound, "minecraft:chests/village/village_toolsmith");
        compound = SharedBlockEntityRootNbt.Fields.LOOT_TABLE_SEED.write(compound, 42L);

        assertEquals("mojang_block_entity_root", SharedBlockEntityRootNbt.NAME);
        assertEquals(List.of("id", "x", "y", "z"), SharedBlockEntityRootNbt.SCHEMA.fields().stream()
            .filter(field -> field.key().equals("id") || field.key().equals("x") || field.key().equals("y") || field.key().equals("z"))
            .map(RNbtField::key)
            .toList());
        assertTrue(SharedBlockEntityRootNbt.SCHEMA.fields().contains(SharedBlockEntityRootNbt.Fields.ITEMS));
        assertEquals("minecraft:chest", SharedBlockEntityRootNbt.Fields.ID.read(compound).orElseThrow());
        assertEquals(64, SharedBlockEntityRootNbt.Fields.Y.read(compound).orElseThrow());
        assertEquals(3, SharedBlockEntityRootNbt.Paths.ITEMS.read(compound).orElseThrow().getFirst().get("count").orElseThrow().asPrimitive().intValue());
        assertEquals(Component.text("Vault"), SharedBlockEntityRootNbt.Paths.CUSTOM_NAME.read(compound).orElseThrow());
        assertEquals("secret", SharedBlockEntityRootNbt.Fields.LOCK.read(compound).orElseThrow());
        assertEquals("minecraft:chests/village/village_toolsmith", SharedBlockEntityRootNbt.Fields.LOOT_TABLE.read(compound).orElseThrow());
        assertEquals(42L, SharedBlockEntityRootNbt.Fields.LOOT_TABLE_SEED.read(compound).orElseThrow());

        RNbtCompound stripped = compound.remove(SharedBlockEntityRootNbt.Paths.ITEMS).remove(SharedBlockEntityRootNbt.Paths.CUSTOM_NAME);

        assertFalse(SharedBlockEntityRootNbt.Paths.ITEMS.exists(stripped));
        assertFalse(SharedBlockEntityRootNbt.Paths.CUSTOM_NAME.exists(stripped));
        assertTrue(SharedBlockEntityRootNbt.Paths.LOCK.exists(stripped));
    }
}
