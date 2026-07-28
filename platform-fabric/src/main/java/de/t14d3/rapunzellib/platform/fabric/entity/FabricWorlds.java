package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldsCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public final class FabricWorlds extends SharedWorldsCore<FabricWorld> {
    private final SharedAttachmentService attachmentService;

    public FabricWorlds(@NotNull SharedAttachmentService attachmentService, MinecraftServer server) {
        super(server);
        this.attachmentService = attachmentService;
    }

    @Override
    protected @NotNull FabricWorld createWorldWrapper(@NotNull ServerLevel level) {
        return new FabricWorld(level, attachmentService, this);
    }

    @Override
    protected void updateWorldWrapper(@NotNull FabricWorld existingWorld, @NotNull ServerLevel level) {
        existingWorld.updateHandle(level);
    }
}
