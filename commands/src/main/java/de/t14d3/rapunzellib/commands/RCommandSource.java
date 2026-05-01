package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RAudience;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public interface RCommandSource extends RAudience {
    @NotNull Optional<RPlayer> player();

    default @NotNull Audience systemAudience() {
        return audience();
    }

    default @NotNull Audience failureAudience() {
        return audience();
    }

    default void sendSystemMessage(@NotNull Component message) {
        systemAudience().sendMessage(message);
    }

    default void sendFailure(@NotNull Component message) {
        failureAudience().sendMessage(message);
    }

    default boolean hasPermission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        if (permission.isBlank()) {
            return true;
        }
        return player().map(player -> player.hasPermission(permission)).orElse(false);
    }

    default boolean isPlayer() {
        return player().isPresent();
    }

    default @NotNull RPlayer requirePlayer() {
        return player().orElseThrow(() -> new IllegalStateException("Command source is not a player"));
    }
}
