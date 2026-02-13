package de.t14d3.rapunzellib.nbt.shared;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.NotNull;

public final class SharedAdventureComponentCodec {
    private SharedAdventureComponentCodec() {
    }

    public static @NotNull net.minecraft.network.chat.Component toNative(@NotNull Component adventure) {
        String json = GsonComponentSerializer.gson().serialize(adventure);
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    public static @NotNull Component toAdventure(@NotNull net.minecraft.network.chat.Component nativeComponent) {
        return GsonComponentSerializer.gson().deserialize(
            ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, nativeComponent).getOrThrow().toString()
        );
    }
}
