package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldBase;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

final class FabricWorld extends SharedWorldBase {
    FabricWorld(ServerLevel world, FabricWorlds worlds) {
        super(PlatformId.FABRIC, Objects.requireNonNull(world, "world"), de.t14d3.rapunzellib.attachments.RAttachmentContainer.lazyMutable(), Objects.requireNonNull(worlds, "worlds"));
    }

    void updateHandle(ServerLevel newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }
}
