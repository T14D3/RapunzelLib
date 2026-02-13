package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.platform.shared.entity.SharedPlayersCore;
import de.t14d3.rapunzellib.platform.paper.PaperHandleBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PaperPlayers extends SharedPlayersCore<PaperPlayer> {
    private final PaperWorlds worlds;

    public PaperPlayers(MinecraftServer server, @NotNull PaperWorlds worlds) {
        super(server);
        this.worlds = worlds;
    }

    @Override
    protected @NotNull PaperPlayer createWrapper(@NotNull ServerPlayer player) {
        return new PaperPlayer(player, worlds);
    }

    @Override
    protected void updateWrapper(@NotNull PaperPlayer existingPlayer, @NotNull ServerPlayer player) {
        existingPlayer.updateHandle(player);
    }


    @Override
    protected @NotNull java.util.Optional<? extends ServerPlayer> adaptNativePlayer(@NotNull Object nativePlayer) {
        if (nativePlayer instanceof Player player) {
            return java.util.Optional.of(PaperHandleBridge.toNms(player));
        }
        return super.adaptNativePlayer(nativePlayer);
    }
}
