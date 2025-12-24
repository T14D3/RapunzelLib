package de.t14d3.rapunzellib.objects;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public interface RAudience extends RNative {
    Audience audience();

    default void sendMessage(Component message) {
        audience().sendMessage(message);
    }

    default void sendActionBar(Component message) {
        audience().sendActionBar(message);
    }
}

