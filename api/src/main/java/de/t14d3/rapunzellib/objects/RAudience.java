package de.t14d3.rapunzellib.objects;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface RAudience extends RNative {
    @NotNull Audience audience();

    default void sendMessage(@NotNull Component message) {
        audience().sendMessage(message);
    }

    default void sendActionBar(@NotNull Component message) {
        audience().sendActionBar(message);
    }
}

