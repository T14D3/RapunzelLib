package de.t14d3.rapunzellib;

import de.t14d3.rapunzellib.objects.RKey;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RKeyTest {
    @Test
    void parsesNamespacedKeysIntoNamespaceAndPath() {
        RKey key = RKey.parse("minecraft:stone");

        assertEquals("minecraft", key.namespace());
        assertEquals("stone", key.path());
        assertEquals("minecraft:stone", key.asString());
        assertEquals("minecraft:stone", key.toString());
    }

    @Test
    void tryParseRejectsInvalidKeysWithoutThrowing() {
        Optional<RKey> key = RKey.tryParse("not-a-key");

        assertTrue(key.isEmpty());
        assertFalse(RKey.isValid("not-a-key"));
    }

    @Test
    void rejectsBlankOrMalformedSegments() {
        assertThrows(IllegalArgumentException.class, () -> RKey.parse(""));
        assertThrows(IllegalArgumentException.class, () -> RKey.parse("minecraft:"));
        assertThrows(IllegalArgumentException.class, () -> RKey.of("minecraft", "bad:path"));
        assertTrue(RKey.isValid("test-domain:path/with.parts"));
    }
}
