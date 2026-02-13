package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.Living;

final class SpongeLivingEntity extends SpongeEntity implements RLivingEntity {
    SpongeLivingEntity(Living handle, SpongeAttachmentService attachmentService, SpongeWorlds worlds) {
        super(handle, attachmentService, worlds);
    }

    @Override
    public double health() {
        return handle(Living.class).health().get();
    }

    @Override
    public double maxHealth() {
        return handle(Living.class).maxHealth().get();
    }

    @Override
    public int remainingAir() {
        return handle(Living.class).requireValue(Keys.REMAINING_AIR).get();
    }

    @Override
    public int maxAir() {
        return handle(Living.class).requireValue(Keys.MAX_AIR).get();
    }

    @Override
    public boolean isAlive() {
        Living living = handle(Living.class);
        return !living.isRemoved() && living.health().get() > 0.0d;
    }

    @Override
    public boolean canDamage() {
        return true;
    }

    @Override
    public boolean damage(double amount) {
        return SpongeEntitySemantics.damage(handle(Living.class), amount);
    }

    @Override
    public boolean canHeal() {
        return true;
    }

    @Override
    public boolean heal(double amount) {
        return SpongeEntitySemantics.heal(handle(Living.class), amount);
    }
}
