package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedEntityBase;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class NeoForgeEntity extends SharedEntityBase {
    NeoForgeEntity(Entity handle, @NotNull SharedAttachmentService attachmentService, NeoForgeWorlds worlds) {
        super(
            PlatformId.NEOFORGE,
            Objects.requireNonNull(handle, "handle"),
            attachmentService.forEntity(handle),
            Objects.requireNonNull(worlds, "worlds")
        );
    }
}