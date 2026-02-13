package de.t14d3.rapunzellib.gui.shared;

import de.t14d3.rapunzellib.nbt.shared.SharedAdventureComponentCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class SharedGuiComponents {
    private SharedGuiComponents() {
    }

    public static @NotNull net.minecraft.network.chat.Component toNative(@NotNull Component component) {
        return SharedAdventureComponentCodec.toNative(component);
    }

    public static @NotNull String plain(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
