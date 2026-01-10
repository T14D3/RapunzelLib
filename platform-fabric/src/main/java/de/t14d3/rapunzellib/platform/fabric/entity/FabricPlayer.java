package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class FabricPlayer extends RNativeHandle<ServerPlayer> implements RPlayer {
    FabricPlayer(ServerPlayer handle) {
        super(PlatformId.FABRIC, Objects.requireNonNull(handle, "handle"));
    }

    void updateHandle(ServerPlayer newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public @NotNull Audience audience() {
        return handle();
    }

    @Override
    public @NotNull UUID uuid() {
        return handle().getUUID();
    }

    @Override
    public @NotNull String name() {
        return handle().getGameProfile().name();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return FabricPermissions.hasPermission(handle(), permission);
    }

    @Override
    public @NotNull Optional<RWorld> world() {
        ServerPlayer player = handle();
        return Rapunzel.context().worlds()
            .wrap(player.level())
            .or(() -> Optional.of(new FabricWorld(player.level())));
    }

    @Override
    public @NotNull Optional<RLocation> location() {
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
    public void teleport(@NotNull RLocation location) {
        Objects.requireNonNull(location, "location");
        handle().teleportTo(location.x(), location.y(), location.z());
    }
}
