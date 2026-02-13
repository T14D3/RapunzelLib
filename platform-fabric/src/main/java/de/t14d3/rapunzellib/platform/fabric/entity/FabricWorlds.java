package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldsCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public final class FabricWorlds extends SharedWorldsCore<FabricWorld> {

    public FabricWorlds(MinecraftServer server) {
        super(server);
    }

    @Override
    protected @NotNull FabricWorld createWorldWrapper(@NotNull ServerLevel level) {
        return new FabricWorld(level, this);
    }

    @Override
    protected void updateWorldWrapper(@NotNull FabricWorld existingWorld, @NotNull ServerLevel level) {
        existingWorld.updateHandle(level);
    }

    @Override
    protected boolean matchesName(@NotNull String name, @NotNull ServerLevel level, @NotNull RKey key) {
        return name.equalsIgnoreCase(level.dimension().identifier().toString()) || name.equalsIgnoreCase(key.asString());
    }
}
