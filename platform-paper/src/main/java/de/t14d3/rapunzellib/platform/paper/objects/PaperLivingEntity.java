package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.platform.shared.entity.SharedEntityOperations;
import de.t14d3.rapunzellib.platform.shared.entity.SharedLivingEntitySupport;
import net.minecraft.world.entity.LivingEntity;

final class PaperLivingEntity extends PaperEntity implements RLivingEntity {
    PaperLivingEntity(LivingEntity handle, PaperWorlds worlds) {
        super(handle, worlds);
    }

    @Override
    public double health() {
        return SharedLivingEntitySupport.health(handle(LivingEntity.class));
    }

    @Override
    public double maxHealth() {
        return SharedLivingEntitySupport.maxHealth(handle(LivingEntity.class));
    }

    @Override
    public int remainingAir() {
        return SharedLivingEntitySupport.remainingAir(handle(LivingEntity.class));
    }

    @Override
    public int maxAir() {
        return SharedLivingEntitySupport.maxAir(handle(LivingEntity.class));
    }

    @Override
    public boolean isAlive() {
        return SharedLivingEntitySupport.isAlive(handle(LivingEntity.class));
    }

    @Override
    public boolean canDamage() {
        return true;
    }

    @Override
    public boolean damage(double amount) {
        return SharedEntityOperations.damage(handle(LivingEntity.class), amount);
    }

    @Override
    public boolean canHeal() {
        return true;
    }

    @Override
    public boolean heal(double amount) {
        return SharedEntityOperations.heal(handle(LivingEntity.class), amount);
    }
}
