package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedPlayersCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class FabricPlayers extends SharedPlayersCore<FabricPlayer> {
    private final SharedAttachmentService attachmentService;
    private final FabricWorlds worlds;

    public FabricPlayers(@NotNull SharedAttachmentService attachmentService, MinecraftServer server, @NotNull FabricWorlds worlds) {
        super(server);
        this.attachmentService = attachmentService;
        this.worlds = worlds;
    }

    @Override
    protected @NotNull FabricPlayer createWrapper(@NotNull ServerPlayer player) {
        return new FabricPlayer(player, attachmentService, worlds);
    }

    @Override
    protected void updateWrapper(@NotNull FabricPlayer existingPlayer, @NotNull ServerPlayer player) {
        existingPlayer.updateHandle(player);
    }
}
