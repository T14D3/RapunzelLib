package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedLivingEntityBase;
import net.minecraft.world.entity.LivingEntity;

final class FabricLivingEntity extends SharedLivingEntityBase {
    FabricLivingEntity(LivingEntity handle, FabricWorlds worlds) {
        super(PlatformId.FABRIC, handle, de.t14d3.rapunzellib.attachments.RAttachmentContainer.lazyMutable(), worlds);
    }
}
