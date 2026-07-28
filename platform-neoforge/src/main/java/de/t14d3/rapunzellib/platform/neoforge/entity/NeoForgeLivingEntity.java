package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.entity.SharedLivingEntityBase;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

final class NeoForgeLivingEntity extends SharedLivingEntityBase {
    NeoForgeLivingEntity(LivingEntity handle, @NotNull SharedAttachmentService attachmentService, NeoForgeWorlds worlds) {
        super(PlatformId.NEOFORGE, handle, attachmentService.forEntity(handle), worlds);
    }
}