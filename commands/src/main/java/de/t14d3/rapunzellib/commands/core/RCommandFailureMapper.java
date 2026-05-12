package de.t14d3.rapunzellib.commands.core;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for mapping {@link RCommandException} instances to Brigadier's {@link CommandSyntaxException}
 * and Adventure {@link net.kyori.adventure.text.Component} messages.
 * <p>
 * Enables the command framework to translate Rapunzel command exceptions into
 * platform-appropriate error representations.
 * </p>
 */
public final class RCommandFailureMapper {
    private RCommandFailureMapper() {
    }

    /**
     * Converts an {@link RCommandException} to a Brigadier {@link CommandSyntaxException}.
     *
     * @param exception the Rapunzel command exception
     * @return the Brigadier syntax exception
     */
    public static @NotNull CommandSyntaxException toSyntaxException(@NotNull RCommandException exception) {
        return new SimpleCommandExceptionType(
            new LiteralMessage(exception.clientMessage())
        ).create();
    }

    /**
     * Converts an {@link RCommandException} to an Adventure {@link Component} message.
     *
     * @param exception the Rapunzel command exception
     * @return the component message
     */
    public static @NotNull Component toSpongeMessage(@NotNull RCommandException exception) {
        return Component.text(exception.clientMessage());
    }

    /**
     * Converts a Brigadier {@link CommandSyntaxException} to an Adventure {@link Component},
     * unwrapping any nested {@link RCommandException}.
     *
     * @param exception the Brigadier syntax exception
     * @return the component message
     */
    public static @NotNull Component toSpongeMessage(@NotNull CommandSyntaxException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof RCommandException commandException) {
            return toSpongeMessage(commandException);
        }
        return Component.text(exception.getMessage());
    }
}
