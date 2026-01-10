package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class NeoForgeWorld extends RNativeHandle<ServerLevel> implements RWorld {
    private final String key;

    NeoForgeWorld(ServerLevel world) {
        super(PlatformId.NEOFORGE, Objects.requireNonNull(world, "world"));
        this.key = world.dimension().location().toString();
    }

    void updateHandle(ServerLevel newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public @NotNull RWorldRef ref() {
        return new RWorldRef(key, key);
    }

    @Override
    public @NotNull Optional<UUID> uuid() {
        return Optional.of(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
    }
}
