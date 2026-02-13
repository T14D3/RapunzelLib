package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.entity.SharedLivingEntityBase;
import net.minecraft.world.entity.LivingEntity;

final class NeoForgeLivingEntity extends SharedLivingEntityBase {
    NeoForgeLivingEntity(LivingEntity handle, NeoForgeWorlds worlds) {
        super(PlatformId.NEOFORGE, handle, de.t14d3.rapunzellib.attachments.RAttachmentContainer.lazyMutable(), worlds);
    }
}
