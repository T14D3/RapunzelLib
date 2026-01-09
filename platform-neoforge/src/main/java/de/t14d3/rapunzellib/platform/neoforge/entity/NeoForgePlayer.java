package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class NeoForgePlayer extends RNativeHandle<ServerPlayer> implements RPlayer {
    NeoForgePlayer(ServerPlayer handle) {
        super(PlatformId.NEOFORGE, Objects.requireNonNull(handle, "handle"));
    }

    void updateHandle(ServerPlayer newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public Audience audience() {
        return (Audience) handle();
    }

    @Override
    public UUID uuid() {
        return handle().getUUID();
    }

    @Override
    public String name() {
        return handle().getGameProfile().name();
    }

    @Override
    public boolean hasPermission(String permission) {
        return NeoForgePermissions.hasPermission(handle(), permission);
    }

    @Override
    public Optional<RWorld> world() {
        ServerPlayer player = handle();
        return Rapunzel.context().worlds()
            .wrap(player.level())
            .or(() -> Optional.of(new NeoForgeWorld(player.level())));
    }

    @Override
    public Optional<RLocation> location() {
        ServerPlayer player = handle();
        String key = player.level().dimension().location().toString();
        RWorldRef worldRef = new RWorldRef(key, key);
        return Optional.of(new RLocation(worldRef, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
    }

    @Override
    public boolean canTeleport() {
        return true;
    }

    @Override
    public void teleport(RLocation location) {
        Objects.requireNonNull(location, "location");
        handle().teleportTo(location.x(), location.y(), location.z());
    }
}
