package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.WrapperStore;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base {@link WrapperStore} implementation for platforms built on NMS
 * {@code ServerLevel} (Fabric, NeoForge).
 *
 * <p>Caches {@link RWorldRef} instances per {@link ServerLevel} via
 * {@link SharedWorldHooks#key(ServerLevel)} for the underlying key, and
 * delegates platform-specific naming to subclasses if needed.</p>
 *
 * <p>Platforms without a dedicated native location type (Fabric, NeoForge)
 * return an empty {@link Optional} from {@link #location(Object)} so callers
 * fall back to coordinate-based {@code RLocation} construction.</p>
 */
public abstract class NmsWrapperStore implements WrapperStore {
    private final ConcurrentHashMap<ServerLevel, RWorldRef> worldRefCache = new ConcurrentHashMap<>();

    @Override
    public @NotNull Optional<RWorldRef> worldRef(@NotNull Object nativeWorld) {
        if (nativeWorld instanceof ServerLevel level) {
            return Optional.of(worldRefCache.computeIfAbsent(level, l ->
                new RWorldRef(null, SharedWorldHooks.key(l))));
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RLocation> location(@NotNull Object nativeLocation) {
        return Optional.empty();
    }
}
