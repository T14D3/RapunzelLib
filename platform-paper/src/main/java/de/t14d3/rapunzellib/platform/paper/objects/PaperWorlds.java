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
import java.util.concurrent.ConcurrentHashMap;

public final class PaperWorlds extends SharedWorldsCore<PaperWorld> {
    private final ConcurrentHashMap<ServerLevel, RWorldRef> worldRefCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ServerLevel, UUID> worldUuidCache = new ConcurrentHashMap<>();

    public PaperWorlds(MinecraftServer server) {
        super(server);
    }

    /**
     * Returns the internal cache of {@link ServerLevel} to {@link RWorldRef},
     * exposed so that {@link PaperWrapperStore} can reuse the same backing
     * cache rather than maintaining a duplicate.
     */
    ConcurrentHashMap<ServerLevel, RWorldRef> worldRefCache() {
        return worldRefCache;
    }

    @Override
    protected @NotNull PaperWorld createWorldWrapper(@NotNull ServerLevel level) {
        return new PaperWorld(level, this, cachedWorldUuid(level));
    }

    @Override
    protected void updateWorldWrapper(@NotNull PaperWorld existingWorld, @NotNull ServerLevel level) {
        existingWorld.updateHandle(level);
    }

    @Override
    public @NotNull RWorldRef worldRef(@NotNull ServerLevel level) {
        return worldRefCache.computeIfAbsent(level, l -> {
            RKey key = SharedWorldHooks.key(l);
            String name = PaperHandleBridge.toBukkit(l).map(World::getName).orElse(key.asString());
            return new RWorldRef(name, key);
        });
    }

    /**
     * Returns the cached UUID for the given world, resolving it once via
     * {@link PaperHandleBridge#worldUuid(ServerLevel)}.
     */
    public @NotNull UUID cachedWorldUuid(@NotNull ServerLevel level) {
        return worldUuidCache.computeIfAbsent(level, PaperHandleBridge::worldUuid);
    }

    @Override
    public @NotNull Optional<UUID> worldUuid(@NotNull ServerLevel level) {
        return Optional.of(cachedWorldUuid(level));
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
