package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedEntityBase;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

class PaperEntity extends SharedEntityBase {
    PaperEntity(Entity handle, PaperWorlds worlds) {
        super(
            PlatformId.PAPER,
            Objects.requireNonNull(handle, "handle"),
            PaperPersistentAttachments.forEntity(handle.getUUID()),
            Objects.requireNonNull(worlds, "worlds")
        );
    }
}
