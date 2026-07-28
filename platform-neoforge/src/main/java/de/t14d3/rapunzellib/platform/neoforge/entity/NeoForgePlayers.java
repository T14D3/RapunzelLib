package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedPlayersCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class NeoForgePlayers extends SharedPlayersCore<NeoForgePlayer> {
    private final SharedAttachmentService attachmentService;
    private final NeoForgeWorlds worlds;

    public NeoForgePlayers(@NotNull SharedAttachmentService attachmentService, MinecraftServer server, @NotNull NeoForgeWorlds worlds) {
        super(server);
        this.attachmentService = attachmentService;
        this.worlds = worlds;
    }

    @Override
    protected @NotNull NeoForgePlayer createWrapper(@NotNull ServerPlayer player) {
        return new NeoForgePlayer(player, attachmentService, worlds);
    }

    @Override
    protected void updateWrapper(@NotNull NeoForgePlayer existingPlayer, @NotNull ServerPlayer player) {
        existingPlayer.updateHandle(player);
    }
}