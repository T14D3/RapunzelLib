package de.t14d3.rapunzellib.platform.velocity.objects;

import com.velocitypowered.api.proxy.Player;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class VelocityPlayer extends RNativeHandle<Player> implements RPlayer {
    VelocityPlayer(Player handle) {
        super(PlatformId.VELOCITY, Objects.requireNonNull(handle, "handle"));
    }

    void updateHandle(Player newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public Audience audience() {
        return handle();
    }

    @Override
    public UUID uuid() {
        return handle().getUniqueId();
    }

    @Override
    public String name() {
        return handle().getUsername();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle().hasPermission(permission);
    }

    @Override
    public Optional<String> currentServerName() {
        return handle().getCurrentServer().map(sc -> sc.getServerInfo().getName());
    }
}

