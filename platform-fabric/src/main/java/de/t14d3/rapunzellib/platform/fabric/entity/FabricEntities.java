package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.platform.shared.entity.SharedEntitiesCore;
import de.t14d3.rapunzellib.platform.shared.entity.SharedEntityBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class FabricEntities extends SharedEntitiesCore<SharedEntityBase> {
    private final FabricWorlds worlds;

    public FabricEntities(@NotNull MinecraftServer server, @NotNull FabricPlayers players, @NotNull FabricWorlds worlds) {
        super(server, players::requireServer);
        this.worlds = worlds;
    }

    @Override
    protected @NotNull SharedEntityBase createEntity(@NotNull Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return new FabricLivingEntity(livingEntity, worlds);
        }
        return new FabricEntity(entity, worlds);
    }

    @Override
    protected void updateEntity(@NotNull SharedEntityBase existingEntity, @NotNull Entity entity) {
        existingEntity.updateHandle(entity);
    }
}
