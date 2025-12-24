package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.bukkit.World;

import java.util.Optional;
import java.util.UUID;

final class PaperWorld extends RNativeHandle<World> implements RWorld {

    PaperWorld(World world) {
        super(PlatformId.PAPER, world);
    }

    void updateHandle(World newHandle) {
        updateNativeHandle(newHandle);
    }

    @Override
    public RWorldRef ref() {
        World world = handle();
        world.getKey();
        String key = world.getKey().toString();
        return new RWorldRef(world.getName(), key);
    }

    @Override
    public Optional<UUID> uuid() {
        return Optional.of(handle().getUID());
    }
}

