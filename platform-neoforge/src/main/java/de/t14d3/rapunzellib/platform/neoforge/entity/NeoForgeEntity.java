package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedEntityBase;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

class NeoForgeEntity extends SharedEntityBase {
    NeoForgeEntity(Entity handle, NeoForgeWorlds worlds) {
        super(
            PlatformId.NEOFORGE,
            Objects.requireNonNull(handle, "handle"),
            de.t14d3.rapunzellib.attachments.RAttachmentContainer.lazyMutable(),
            Objects.requireNonNull(worlds, "worlds")
        );
    }
}
