package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RAudience;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the source of a command execution.
 * <p>
 * Extends {@link RAudience} to provide message-sending capabilities and adds
 * methods for player identification, permission checking, and segmented reply
 * channels (standard, system, failure).
 * </p>
 */
public interface RCommandSource extends RAudience {
    /**
     * Gets the player associated with this command source, if any.
     *
     * @return an optional containing the player, or empty if the source is console/command block
     */
    @NotNull Optional<RPlayer> player();

    default @NotNull Audience systemAudience() {
        return audience();
    }

    default @NotNull Audience failureAudience() {
        return audience();
    }

    /**
     * Sends a system message to the system audience.
     *
     * @param message the message component to send
     */
    default void sendSystemMessage(@NotNull Component message) {
        systemAudience().sendMessage(message);
    }

    /**
     * Sends a failure message to the failure audience.
     *
     * @param message the message component to send
     */
    default void sendFailure(@NotNull Component message) {
        failureAudience().sendMessage(message);
    }

    /**
     * Checks if this source has the specified permission.
     * <p>Blank permissions always return true.</p>
     *
     * @param permission the permission string to check
     * @return true if the source has the permission
     */
    default boolean hasPermission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        if (permission.isBlank()) {
            return true;
        }
        return player().map(player -> player.hasPermission(permission)).orElse(false);
    }

    /**
     * Checks if this source is a player.
     *
     * @return true if a player is associated with this source
     */
    default boolean isPlayer() {
        return player().isPresent();
    }

    /**
     * Requires that this source has an associated player.
     *
     * @return the player
     * @throws IllegalStateException if this source is not a player
     */
    default @NotNull RPlayer requirePlayer() {
        return player().orElseThrow(() -> new IllegalStateException("Command source is not a player"));
    }
}
