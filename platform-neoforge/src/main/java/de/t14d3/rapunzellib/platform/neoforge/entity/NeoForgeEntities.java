package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedEntitiesCore;
import de.t14d3.rapunzellib.platform.shared.entity.SharedEntityBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeEntities extends SharedEntitiesCore<SharedEntityBase> {
    private final SharedAttachmentService attachmentService;
    private final NeoForgeWorlds worlds;

    public NeoForgeEntities(@NotNull SharedAttachmentService attachmentService, @NotNull MinecraftServer server, @NotNull NeoForgePlayers players, @NotNull NeoForgeWorlds worlds) {
        super(server, players::requireServer);
        this.attachmentService = attachmentService;
        this.worlds = worlds;
    }

    @Override
    protected @NotNull SharedEntityBase createEntity(@NotNull Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return new NeoForgeLivingEntity(livingEntity, attachmentService, worlds);
        }
        return new NeoForgeEntity(entity, attachmentService, worlds);
    }

    @Override
    protected void updateEntity(@NotNull SharedEntityBase existingEntity, @NotNull Entity entity) {
        existingEntity.updateHandle(entity);
    }
}