package de.t14d3.rapunzellib.gui.shared;

import de.t14d3.rapunzellib.nbt.shared.SharedAdventureComponentCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Shared utility for converting between Adventure {@link Component}
 * and Minecraft native components, and for plain-text serialization.
 */
public final class SharedGuiComponents {
    private SharedGuiComponents() {
    }

    /**
     * Converts an Adventure component to a Minecraft native component.
     *
     * @param component the Adventure component
     * @return the native component
     */
    public static @NotNull net.minecraft.network.chat.Component toNative(@NotNull Component component) {
        return SharedAdventureComponentCodec.toNative(component);
    }

    /**
     * Serializes a component to plain text.
     *
     * @param component the component
     * @return the plain text string
     */
    public static @NotNull String plain(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
