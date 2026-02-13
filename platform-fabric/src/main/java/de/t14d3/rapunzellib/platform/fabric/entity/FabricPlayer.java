package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedServerPlayerBase;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class FabricPlayer extends SharedServerPlayerBase {
    FabricPlayer(ServerPlayer handle, FabricWorlds worlds) {
        super(
            PlatformId.FABRIC,
            Objects.requireNonNull(handle, "handle"),
            de.t14d3.rapunzellib.attachments.RAttachmentContainer.lazyMutable(),
            Objects.requireNonNull(worlds, "worlds")
        );
    }

    void updateHandle(ServerPlayer newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public @NotNull Audience audience() {
        return (Audience) handle();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return FabricPermissions.hasPermission(handle(), permission);
    }
}
