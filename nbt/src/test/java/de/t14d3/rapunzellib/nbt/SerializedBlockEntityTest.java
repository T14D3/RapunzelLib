package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.objects.RKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SerializedBlockEntityTest {
    @Test
    void base64RoundTripKeepsStructuredNbtPayload() {
        SerializedBlockEntity blockEntity = new SerializedBlockEntity(
            "minecraft:chest",
            RNbtCompound.builder().putString("id", "minecraft:chest").putInt("x", 12).putInt("y", 64).putInt("z", -4).build(),
            Instant.parse("2026-03-09T12:00:00Z"),
            RNbtCompound.builder().putString("source", "test").build()
        );

        SerializedBlockEntity roundTrip = SerializedBlockEntity.fromBase64(blockEntity.toBase64());

        assertEquals(blockEntity, roundTrip);
        assertEquals(RKey.of("minecraft:chest"), roundTrip.blockEntityType());
        assertEquals("minecraft:chest", roundTrip.blockEntityTypeId());
        assertEquals("test", RNbtCodecs.STRING.decode(roundTrip.metadata().get("source").orElseThrow()));
    }
}
