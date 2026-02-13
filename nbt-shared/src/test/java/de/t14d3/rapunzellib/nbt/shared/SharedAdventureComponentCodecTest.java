package de.t14d3.rapunzellib.nbt.shared;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SharedAdventureComponentCodecTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void structuredAdventureComponentsRoundTripThroughSharedCodec() {
        Component original = Component.text()
            .append(Component.text("Hello", NamedTextColor.RED))
            .append(Component.text(" world").decorate(TextDecoration.BOLD))
            .append(Component.text("!", NamedTextColor.GOLD))
            .build();

        Component decoded = SharedAdventureComponentCodec.toAdventure(
            SharedAdventureComponentCodec.toNative(original)
        );

        assertEquals(
            GsonComponentSerializer.gson().serialize(original),
            GsonComponentSerializer.gson().serialize(decoded)
        );
    }
}
