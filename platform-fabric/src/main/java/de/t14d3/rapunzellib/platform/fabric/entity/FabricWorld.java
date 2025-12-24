package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

final class FabricWorld extends RNativeHandle<ServerLevel> implements RWorld {
    private final String key;

    FabricWorld(ServerLevel world) {
        super(PlatformId.FABRIC, Objects.requireNonNull(world, "world"));
        this.key = world.dimension().location().toString();
    }

    void updateHandle(ServerLevel newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public RWorldRef ref() {
        return new RWorldRef(key, key);
    }

    @Override
    public Optional<UUID> uuid() {
        return Optional.of(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
    }
}

