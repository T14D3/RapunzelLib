package de.t14d3.rapunzellib.commands.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.RCommandSources;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class PaperCommandSourceAdapter {
    private PaperCommandSourceAdapter() {
    }

    public static RCommandSource wrap(CommandSender sender) {
        if (sender == null) throw new IllegalArgumentException("sender cannot be null");
        if (!(sender instanceof Audience audience)) {
            throw new IllegalArgumentException(
                "CommandSender does not implement Adventure Audience: " + sender.getClass().getName()
            );
        }

        Optional<RPlayer> player = (sender instanceof Player bukkitPlayer)
            ? Rapunzel.players().wrap(bukkitPlayer)
            : Optional.empty();

        return RCommandSources.of(PlatformId.PAPER, audience, player);
    }
}
