package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class SpongeWorld extends RNativeHandle<ServerWorld> implements RWorld {
    SpongeWorld(ServerWorld handle) {
        super(PlatformId.SPONGE, Objects.requireNonNull(handle, "handle"));
    }

    @Override
    public @NotNull RWorldRef ref() {
        ServerWorld world = handle();
        return new RWorldRef(world.properties().name(), world.key().asString());
    }

    @Override
    public @NotNull Optional<UUID> uuid() {
        return Optional.of(handle().uniqueId());
    }
}
