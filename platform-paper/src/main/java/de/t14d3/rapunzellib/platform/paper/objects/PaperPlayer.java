package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.platform.shared.entity.SharedServerPlayerBase;
import de.t14d3.rapunzellib.platform.paper.PaperHandleBridge;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class PaperPlayer extends SharedServerPlayerBase implements RServerPlayer {
    PaperPlayer(ServerPlayer handle, PaperWorlds worlds) {
        super(
            PlatformId.PAPER,
            Objects.requireNonNull(handle, "handle"),
            PaperPersistentAttachments.forPlayer(handle.getUUID()),
            Objects.requireNonNull(worlds, "worlds")
        );
    }

    void updateHandle(ServerPlayer newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    private @NotNull Player bukkit() {
        return PaperHandleBridge.toBukkit(handle());
    }

    @Override
    public @NotNull Audience audience() {
        return bukkit();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return bukkit().hasPermission(permission);
    }
}
