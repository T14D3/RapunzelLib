package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.platform.shared.entity.SharedEntitiesCore;
import de.t14d3.rapunzellib.platform.paper.PaperHandleBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class PaperEntities extends SharedEntitiesCore<PaperEntity> {
    private final PaperWorlds worlds;

    public PaperEntities(@NotNull MinecraftServer server, @NotNull PaperPlayers players, @NotNull PaperWorlds worlds) {
        super(server, players::requireServer);
        this.worlds = worlds;
    }

    @Override
    protected @NotNull PaperEntity createEntity(@NotNull Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return new PaperLivingEntity(livingEntity, worlds);
        }
        return new PaperEntity(entity, worlds);
    }

    @Override
    protected void updateEntity(@NotNull PaperEntity existingEntity, @NotNull Entity entity) {
        existingEntity.updateHandle(entity);
    }

    @Override
    protected @NotNull java.util.Optional<? extends Entity> adaptNativeEntity(@NotNull Object nativeEntity) {
        if (nativeEntity instanceof org.bukkit.entity.Player player) {
            return java.util.Optional.of(PaperHandleBridge.toNms(player));
        }
        if (nativeEntity instanceof org.bukkit.entity.Entity entity) {
            return java.util.Optional.of(PaperHandleBridge.toNms(entity));
        }
        return super.adaptNativeEntity(nativeEntity);
    }
}
