package de.t14d3.rapunzellib.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class RWorldRefTest {
    @Test
    void parsesStringKeysIntoTypedKeys() {
        RWorldRef ref = new RWorldRef("spawn", "minecraft:overworld");

        assertEquals("spawn", ref.name());
        assertEquals(RKey.of("minecraft:overworld"), ref.key());
        assertEquals("minecraft:overworld", ref.identifier());
    }

    @Test
    void supportsKeyOnlyRefs() {
        RWorldRef ref = new RWorldRef(RKey.of("test:world"));

        assertNull(ref.name());
        assertEquals(RKey.of("test:world"), ref.key());
        assertEquals("test:world", ref.identifier());
    }
}
