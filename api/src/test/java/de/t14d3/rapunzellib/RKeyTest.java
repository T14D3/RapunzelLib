package de.t14d3.rapunzellib;

import de.t14d3.rapunzellib.objects.RKey;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void allFactoriesInternToTheSameInstance() {
        RKey fromParse = RKey.parse("minecraft:stone");
        RKey fromOfString = RKey.of("minecraft:stone");
        RKey fromOfParts = RKey.of("minecraft", "stone");
        RKey fromTryParse = RKey.tryParse("minecraft:stone").orElseThrow();

        assertSame(fromParse, fromOfString);
        assertSame(fromParse, fromOfParts);
        assertSame(fromParse, fromTryParse);
    }

    @Test
    void parsingTrimsAndInternsToTheSameInstance() {
        RKey trimmed = RKey.parse("minecraft:stone");
        RKey withWhitespace = RKey.parse("  minecraft:stone  ");

        assertSame(trimmed, withWhitespace);
    }

    @Test
    void tryParseRejectsEmptyAndPropagatesNull() {
        assertTrue(RKey.tryParse("").isEmpty());
        assertTrue(RKey.tryParse("   ").isEmpty());
        assertThrows(NullPointerException.class, () -> RKey.tryParse(null));
    }

    @Test
    void serializationRoundTripsBackIntoTheInternTable() throws Exception {
        RKey original = RKey.of("minecraft", "diamond_ore");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        RKey deserialized = (RKey) in.readObject();

        assertNotNull(deserialized);
        assertEquals(original, deserialized);
        assertSame(original, deserialized);
    }

    @Test
    void equalsAndHashCodeAreBasedOnComponents() {
        RKey a = RKey.of("a", "b");
        RKey b = RKey.of("a", "b");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
