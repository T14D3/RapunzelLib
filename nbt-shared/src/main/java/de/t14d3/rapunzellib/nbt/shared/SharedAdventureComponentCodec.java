package de.t14d3.rapunzellib.nbt.shared;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.NotNull;

/**
 * Codec for converting between Adventure {@link Component} and Minecraft
 * native {@link net.minecraft.network.chat.Component}.
 * <p>
 * Both conversions pass through JSON serialization for compatibility.
 */
public final class SharedAdventureComponentCodec {
    private SharedAdventureComponentCodec() {
    }

    /**
     * Converts an Adventure component to a Minecraft native component.
     *
     * @param adventure the Adventure component
     * @return the native component
     */
    public static @NotNull net.minecraft.network.chat.Component toNative(@NotNull Component adventure) {
        String json = GsonComponentSerializer.gson().serialize(adventure);
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    /**
     * Converts a Minecraft native component to an Adventure component.
     *
     * @param nativeComponent the native component
     * @return the Adventure component
     */
    public static @NotNull Component toAdventure(@NotNull net.minecraft.network.chat.Component nativeComponent) {
        return GsonComponentSerializer.gson().deserialize(
            ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, nativeComponent).getOrThrow().toString()
        );
    }
}
