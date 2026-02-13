package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldBase;
import de.t14d3.rapunzellib.platform.paper.PaperHandleBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

final class PaperWorld extends SharedWorldBase {
    PaperWorld(ServerLevel world, PaperWorlds worlds) {
        super(
            PlatformId.PAPER,
            Objects.requireNonNull(world, "world"),
            PaperPersistentAttachments.forWorld(PaperHandleBridge.worldUuid(world)),
            Objects.requireNonNull(worlds, "worlds")
        );
    }

    void updateHandle(ServerLevel newHandle) {
        updateNativeHandle(newHandle);
    }
}
