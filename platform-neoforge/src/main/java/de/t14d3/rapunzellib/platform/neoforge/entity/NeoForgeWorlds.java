package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldsCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeWorlds extends SharedWorldsCore<NeoForgeWorld> {

    public NeoForgeWorlds(MinecraftServer server) {
        super(server);
    }

    @Override
    protected @NotNull NeoForgeWorld createWorldWrapper(@NotNull ServerLevel level) {
        return new NeoForgeWorld(level, this);
    }

    @Override
    protected void updateWorldWrapper(@NotNull NeoForgeWorld existingWorld, @NotNull ServerLevel level) {
        existingWorld.updateHandle(level);
    }
}
