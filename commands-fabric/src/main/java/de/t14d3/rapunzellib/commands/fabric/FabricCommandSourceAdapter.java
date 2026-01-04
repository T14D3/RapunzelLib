package de.t14d3.rapunzellib.commands.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.RCommandSources;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class FabricCommandSourceAdapter {
    private FabricCommandSourceAdapter() {
    }

    public static RCommandSource wrap(CommandSourceStack source) {
        Objects.requireNonNull(source, "source");

        ServerPlayer nativePlayer;
        try {
            nativePlayer = source.getPlayer();
        } catch (Exception ignored) {
            nativePlayer = null;
        }
        if (nativePlayer == null) {
            return RCommandSources.of(PlatformId.FABRIC, Audience.empty());
        }

        RPlayer player = Rapunzel.players().require(nativePlayer);
        return RCommandSources.of(PlatformId.FABRIC, player.audience(), player);
    }
}
