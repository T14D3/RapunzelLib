package de.t14d3.rapunzellib.commands.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.AudienceCommandSourceAdapterCore;
import de.t14d3.rapunzellib.commands.RCommandSource;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class PaperBrigadierCommandSourceAdapter {
    private PaperBrigadierCommandSourceAdapter() {
    }

    public static @NotNull RCommandSource wrap(@NotNull CommandSourceStack source) {
        if (source == null) throw new IllegalArgumentException("source cannot be null");

        return AudienceCommandSourceAdapterCore.wrap(
            PlatformId.PAPER,
            source,
            commandSource -> audience(commandSource.getSender()),
            (commandSource, permission) -> commandSource.getSender().hasPermission(permission),
            commandSource -> player(commandSource.getExecutor(), commandSource.getSender())
        );
    }

    private static @NotNull Audience audience(@NotNull CommandSender sender) {
        return sender;
    }

    private static @NotNull Optional<de.t14d3.rapunzellib.objects.RPlayer> player(Object executor, CommandSender sender) {
        if (executor instanceof Player player) {
            return Rapunzel.players().wrap(player);
        }
        if (sender instanceof Player player) {
            return Rapunzel.players().wrap(player);
        }
        return Optional.empty();
    }
}
