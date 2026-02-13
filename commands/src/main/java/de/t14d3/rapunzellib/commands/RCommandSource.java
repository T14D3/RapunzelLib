package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RAudience;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public interface RCommandSource extends RAudience {
    @NotNull Optional<RPlayer> player();

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
