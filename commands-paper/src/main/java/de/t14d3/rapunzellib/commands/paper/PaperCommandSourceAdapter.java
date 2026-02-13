package de.t14d3.rapunzellib.commands.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.AudienceCommandSourceAdapterCore;
import de.t14d3.rapunzellib.commands.RCommandSource;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PaperCommandSourceAdapter {
    private PaperCommandSourceAdapter() {
    }

    public static RCommandSource wrap(CommandSender sender) {
        if (sender == null) throw new IllegalArgumentException("sender cannot be null");
        if (!(sender instanceof Audience)) {
            throw new IllegalArgumentException(
                "CommandSender does not implement Adventure Audience: " + sender.getClass().getName()
            );
        }
        return AudienceCommandSourceAdapterCore.wrap(
            PlatformId.PAPER,
            sender,
            source -> (Audience) source,
            CommandSender::hasPermission,
            source -> (source instanceof Player bukkitPlayer)
                ? Rapunzel.players().wrap(bukkitPlayer)
                : java.util.Optional.empty()
        );
    }
}
