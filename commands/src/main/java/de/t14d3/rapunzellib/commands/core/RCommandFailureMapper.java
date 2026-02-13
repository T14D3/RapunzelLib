package de.t14d3.rapunzellib.commands.core;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class RCommandFailureMapper {
    private RCommandFailureMapper() {
    }

    public static @NotNull CommandSyntaxException toSyntaxException(@NotNull RCommandException exception) {
        return new SimpleCommandExceptionType(
            new LiteralMessage(exception.clientMessage())
        ).create();
    }

    public static @NotNull Component toSpongeMessage(@NotNull RCommandException exception) {
        return Component.text(exception.clientMessage());
    }

    public static @NotNull Component toSpongeMessage(@NotNull CommandSyntaxException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof RCommandException commandException) {
            return toSpongeMessage(commandException);
        }
        return Component.text(exception.getMessage());
    }
}
