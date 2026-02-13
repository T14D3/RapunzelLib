package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedEntityBase;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

class FabricEntity extends SharedEntityBase {
    FabricEntity(Entity handle, FabricWorlds worlds) {
        super(
            PlatformId.FABRIC,
            Objects.requireNonNull(handle, "handle"),
            de.t14d3.rapunzellib.attachments.RAttachmentContainer.lazyMutable(),
            Objects.requireNonNull(worlds, "worlds")
        );
    }
}
