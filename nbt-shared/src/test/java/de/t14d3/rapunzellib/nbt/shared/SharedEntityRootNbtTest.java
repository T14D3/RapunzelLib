package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.nbt.generated.EntityRootNbt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EntityRootNbtTest {
    @Test
    void generatedEntityRootBundleProvidesTypedFieldAndPathAccess() {
        RNbtCompound compound = RNbtCompound.empty();
        compound = EntityRootNbt.Fields.POS.write(compound, List.of(1.5, 64.0, -20.25));
        compound = EntityRootNbt.Fields.ROTATION.write(compound, List.of(90.0f, 15.0f));
        compound = EntityRootNbt.Fields.ATTRIBUTES.write(
            compound,
            List.of(RNbtCompound.builder().putString("id", "minecraft:generic.max_health").putDouble("base", 20.0d).build())
        );
        compound = EntityRootNbt.Fields.BRAIN.write(compound, RNbtCompound.builder().putString("status", "idle").build());
        compound = EntityRootNbt.Fields.HEALTH.write(compound, 20.0f);
        compound = EntityRootNbt.Fields.VARIANT.write(compound, RNbtValue.string("minecraft:warm"));
        compound = EntityRootNbt.Fields.VARIANT_2.write(compound, RNbtValue.intValue(3));
        compound = EntityRootNbt.Fields.AIR.write(compound, (short) 300);
        compound = EntityRootNbt.Fields.FALL_DISTANCE.write(compound, 2.5f);
        compound = EntityRootNbt.Fields.PERSISTENCE_REQUIRED.write(compound, true);
        compound = EntityRootNbt.Fields.INVULNERABLE.write(compound, false);
        compound = EntityRootNbt.Fields.PORTAL_COOLDOWN.write(compound, 120);
        compound = EntityRootNbt.Fields.AGE.write(compound, -24000);
        compound = EntityRootNbt.Fields.WORLD_UUID_MOST.write(compound, 42L);

        assertEquals("mojang_entity_root", EntityRootNbt.NAME);
        assertTrue(EntityRootNbt.SCHEMA.fields().contains(EntityRootNbt.Fields.BRAIN));
        assertTrue(EntityRootNbt.SCHEMA.fields().contains(EntityRootNbt.Fields.PORTAL_COOLDOWN));
        assertEquals(List.of("variant", "Variant"), EntityRootNbt.SCHEMA.fields().stream()
            .filter(field -> field.key().equals("variant") || field.key().equals("Variant"))
            .map(RNbtField::key)
            .toList());
        assertEquals(List.of(1.5, 64.0, -20.25), EntityRootNbt.Fields.POS.read(compound).orElseThrow());
        assertEquals(List.of(90.0f, 15.0f), EntityRootNbt.Paths.ROTATION.read(compound).orElseThrow());
        assertEquals(20.0d, EntityRootNbt.Fields.ATTRIBUTES.read(compound).orElseThrow().getFirst().get("base").orElseThrow().asPrimitive().doubleValue());
        assertEquals("idle", EntityRootNbt.Paths.BRAIN.read(compound).orElseThrow().get("status").orElseThrow().asPrimitive().stringValue());
        assertEquals(20.0f, EntityRootNbt.Fields.HEALTH.read(compound).orElseThrow());
        assertEquals("minecraft:warm", EntityRootNbt.Paths.VARIANT.read(compound).orElseThrow().asPrimitive().stringValue());
        assertEquals(3, EntityRootNbt.Paths.VARIANT_2.read(compound).orElseThrow().asPrimitive().intValue());
        assertEquals((short) 300, EntityRootNbt.Fields.AIR.read(compound).orElseThrow());
        assertEquals(2.5f, EntityRootNbt.Fields.FALL_DISTANCE.read(compound).orElseThrow());
        assertTrue(EntityRootNbt.Fields.PERSISTENCE_REQUIRED.read(compound).orElseThrow());
        assertFalse(EntityRootNbt.Fields.INVULNERABLE.read(compound).orElseThrow());
        assertEquals(120, EntityRootNbt.Fields.PORTAL_COOLDOWN.read(compound).orElseThrow());
        assertEquals(-24000, EntityRootNbt.Fields.AGE.read(compound).orElseThrow());
        assertTrue(EntityRootNbt.Paths.WORLD_UUID_MOST.exists(compound));

        RNbtCompound stripped = compound.remove(EntityRootNbt.Paths.POS).remove(EntityRootNbt.Paths.BRAIN);

        assertFalse(EntityRootNbt.Paths.POS.exists(stripped));
        assertFalse(EntityRootNbt.Paths.BRAIN.exists(stripped));
        assertEquals(42L, EntityRootNbt.Fields.WORLD_UUID_MOST.read(stripped).orElseThrow());
    }
}
