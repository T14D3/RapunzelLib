package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedWorldBase;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class NeoForgeWorld extends SharedWorldBase {
    NeoForgeWorld(ServerLevel world, @NotNull SharedAttachmentService attachmentService, NeoForgeWorlds worlds) {
        super(PlatformId.NEOFORGE, Objects.requireNonNull(world, "world"), attachmentService.forWorld(world), Objects.requireNonNull(worlds, "worlds"));
    }

    void updateHandle(ServerLevel newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }
}