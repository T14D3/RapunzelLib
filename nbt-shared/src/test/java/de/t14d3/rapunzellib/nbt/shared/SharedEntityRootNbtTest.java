package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.nbt.shared.generated.SharedEntityRootNbt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SharedEntityRootNbtTest {
    @Test
    void generatedEntityRootBundleProvidesTypedFieldAndPathAccess() {
        RNbtCompound compound = RNbtCompound.empty();
        compound = SharedEntityRootNbt.Fields.POS.write(compound, List.of(1.5, 64.0, -20.25));
        compound = SharedEntityRootNbt.Fields.ROTATION.write(compound, List.of(90.0f, 15.0f));
        compound = SharedEntityRootNbt.Fields.ATTRIBUTES.write(
            compound,
            List.of(RNbtCompound.builder().putString("id", "minecraft:generic.max_health").putDouble("base", 20.0d).build())
        );
        compound = SharedEntityRootNbt.Fields.BRAIN.write(compound, RNbtCompound.builder().putString("status", "idle").build());
        compound = SharedEntityRootNbt.Fields.HEALTH.write(compound, 20.0f);
        compound = SharedEntityRootNbt.Fields.VARIANT.write(compound, RNbtValue.string("minecraft:warm"));
        compound = SharedEntityRootNbt.Fields.VARIANT_2.write(compound, RNbtValue.intValue(3));
        compound = SharedEntityRootNbt.Fields.AIR.write(compound, (short) 300);
        compound = SharedEntityRootNbt.Fields.FALL_DISTANCE.write(compound, 2.5f);
        compound = SharedEntityRootNbt.Fields.PERSISTENCE_REQUIRED.write(compound, true);
        compound = SharedEntityRootNbt.Fields.INVULNERABLE.write(compound, false);
        compound = SharedEntityRootNbt.Fields.PORTAL_COOLDOWN.write(compound, 120);
        compound = SharedEntityRootNbt.Fields.AGE.write(compound, -24000);
        compound = SharedEntityRootNbt.Fields.WORLD_UUID_MOST.write(compound, 42L);

        assertEquals("mojang_entity_root", SharedEntityRootNbt.NAME);
        assertTrue(SharedEntityRootNbt.SCHEMA.fields().contains(SharedEntityRootNbt.Fields.BRAIN));
        assertTrue(SharedEntityRootNbt.SCHEMA.fields().contains(SharedEntityRootNbt.Fields.PORTAL_COOLDOWN));
        assertEquals(List.of("variant", "Variant"), SharedEntityRootNbt.SCHEMA.fields().stream()
            .filter(field -> field.key().equals("variant") || field.key().equals("Variant"))
            .map(RNbtField::key)
            .toList());
        assertEquals(List.of(1.5, 64.0, -20.25), SharedEntityRootNbt.Fields.POS.read(compound).orElseThrow());
        assertEquals(List.of(90.0f, 15.0f), SharedEntityRootNbt.Paths.ROTATION.read(compound).orElseThrow());
        assertEquals(20.0d, SharedEntityRootNbt.Fields.ATTRIBUTES.read(compound).orElseThrow().getFirst().get("base").orElseThrow().asPrimitive().doubleValue());
        assertEquals("idle", SharedEntityRootNbt.Paths.BRAIN.read(compound).orElseThrow().get("status").orElseThrow().asPrimitive().stringValue());
        assertEquals(20.0f, SharedEntityRootNbt.Fields.HEALTH.read(compound).orElseThrow());
        assertEquals("minecraft:warm", SharedEntityRootNbt.Paths.VARIANT.read(compound).orElseThrow().asPrimitive().stringValue());
        assertEquals(3, SharedEntityRootNbt.Paths.VARIANT_2.read(compound).orElseThrow().asPrimitive().intValue());
        assertEquals((short) 300, SharedEntityRootNbt.Fields.AIR.read(compound).orElseThrow());
        assertEquals(2.5f, SharedEntityRootNbt.Fields.FALL_DISTANCE.read(compound).orElseThrow());
        assertTrue(SharedEntityRootNbt.Fields.PERSISTENCE_REQUIRED.read(compound).orElseThrow());
        assertFalse(SharedEntityRootNbt.Fields.INVULNERABLE.read(compound).orElseThrow());
        assertEquals(120, SharedEntityRootNbt.Fields.PORTAL_COOLDOWN.read(compound).orElseThrow());
        assertEquals(-24000, SharedEntityRootNbt.Fields.AGE.read(compound).orElseThrow());
        assertTrue(SharedEntityRootNbt.Paths.WORLD_UUID_MOST.exists(compound));

        RNbtCompound stripped = compound.remove(SharedEntityRootNbt.Paths.POS).remove(SharedEntityRootNbt.Paths.BRAIN);

        assertFalse(SharedEntityRootNbt.Paths.POS.exists(stripped));
        assertFalse(SharedEntityRootNbt.Paths.BRAIN.exists(stripped));
        assertEquals(42L, SharedEntityRootNbt.Fields.WORLD_UUID_MOST.read(stripped).orElseThrow());
    }
}
