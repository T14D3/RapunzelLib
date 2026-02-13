package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.REntityType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SerializedEntityTest {
    @Test
    void base64RoundTripKeepsStructuredNbtPayload() {
        SerializedEntity passenger = new SerializedEntity(
            "minecraft:chicken",
            RNbtCompound.builder().putString("id", "minecraft:chicken").build(),
            List.of(),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            Instant.parse("2026-03-09T12:01:00Z"),
            RNbtCompound.empty()
        );
        SerializedEntity entity = new SerializedEntity(
            "minecraft:zombie",
            RNbtCompound.builder().putString("id", "minecraft:zombie").putInt("Health", 20).build(),
            List.of(passenger),
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            Instant.parse("2026-03-09T12:00:00Z"),
            RNbtCompound.builder().putString("source", "test").build()
        );

        SerializedEntity roundTrip = SerializedEntity.fromBase64(entity.toBase64());

        assertEquals(entity, roundTrip);
        assertEquals(REntityType.ref("minecraft:zombie"), roundTrip.entityType());
        assertEquals(RKey.of("minecraft:zombie"), roundTrip.entityTypeKey());
        assertEquals("minecraft:zombie", roundTrip.entityTypeId());
        assertEquals("test", RNbtCodecs.STRING.decode(roundTrip.metadata().get("source").orElseThrow()));
    }
}
