package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldBase;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.UUID;

final class PaperWorld extends SharedWorldBase {
    PaperWorld(ServerLevel world, PaperWorlds worlds, UUID worldUuid) {
        super(
            PlatformId.PAPER,
            Objects.requireNonNull(world, "world"),
            PaperPersistentAttachments.forWorld(worldUuid),
            Objects.requireNonNull(worlds, "worlds")
        );
    }

    void updateHandle(ServerLevel newHandle) {
        updateNativeHandle(newHandle);
    }
}
