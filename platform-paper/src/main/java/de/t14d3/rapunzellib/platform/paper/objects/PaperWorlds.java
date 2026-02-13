package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldHooks;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldsCore;
import de.t14d3.rapunzellib.platform.paper.PaperHandleBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public final class PaperWorlds extends SharedWorldsCore<PaperWorld> {
    public PaperWorlds(MinecraftServer server) {
        super(server);
    }

    @Override
    protected @NotNull PaperWorld createWorldWrapper(@NotNull ServerLevel level) {
        return new PaperWorld(level, this);
    }

    @Override
    protected void updateWorldWrapper(@NotNull PaperWorld existingWorld, @NotNull ServerLevel level) {
        existingWorld.updateHandle(level);
    }

    @Override
    public @NotNull RWorldRef worldRef(@NotNull ServerLevel level) {
        RKey key = SharedWorldHooks.key(level);
        String name = PaperHandleBridge.toBukkit(level).map(World::getName).orElse(key.asString());
        return new RWorldRef(name, key);
    }

    @Override
    public @NotNull Optional<UUID> worldUuid(@NotNull ServerLevel level) {
        return Optional.of(PaperHandleBridge.worldUuid(level));
    }

    @Override
    protected boolean matchesName(@NotNull String name, @NotNull ServerLevel level, @NotNull RKey key) {
        if (super.matchesName(name, level, key)) return true;
        return PaperHandleBridge.toBukkit(level)
            .map(world -> name.equalsIgnoreCase(world.getName()))
            .orElse(false);
    }

    @Override
    protected @NotNull Optional<? extends ServerLevel> adaptNativeWorld(@NotNull Object nativeWorld) {
        if (nativeWorld instanceof World world) {
            return Optional.of(PaperHandleBridge.toNms(world));
        }
        return super.adaptNativeWorld(nativeWorld);
    }
}
