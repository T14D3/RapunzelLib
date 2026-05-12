package de.t14d3.rapunzellib.objects;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a native object that can receive Adventure messages.
 */
public interface RAudience extends RNative {
    /**
     * Returns the Adventure audience backing this object.
     *
     * @return the audience instance
     */
    @NotNull Audience audience();

    /**
     * Sends a chat message to this audience.
     *
     * @param message the component to send
     */
    default void sendMessage(@NotNull Component message) {
        audience().sendMessage(message);
    }

    /**
     * Sends an action bar message to this audience.
     *
     * @param message the component to send
     */
    default void sendActionBar(@NotNull Component message) {
        audience().sendActionBar(message);
    }
}

