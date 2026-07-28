package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldsCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeWorlds extends SharedWorldsCore<NeoForgeWorld> {
    private final SharedAttachmentService attachmentService;

    public NeoForgeWorlds(@NotNull SharedAttachmentService attachmentService, MinecraftServer server) {
        super(server);
        this.attachmentService = attachmentService;
    }

    @Override
    protected @NotNull NeoForgeWorld createWorldWrapper(@NotNull ServerLevel level) {
        return new NeoForgeWorld(level, attachmentService, this);
    }

    @Override
    protected void updateWorldWrapper(@NotNull NeoForgeWorld existingWorld, @NotNull ServerLevel level) {
        existingWorld.updateHandle(level);
    }
}