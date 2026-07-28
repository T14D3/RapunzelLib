package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedLivingEntityBase;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

final class FabricLivingEntity extends SharedLivingEntityBase {
    FabricLivingEntity(LivingEntity handle, @NotNull SharedAttachmentService attachmentService, FabricWorlds worlds) {
        super(PlatformId.FABRIC, handle, attachmentService.forEntity(handle), worlds);
    }
}
