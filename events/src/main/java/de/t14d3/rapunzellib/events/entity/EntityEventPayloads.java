package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class EntityEventPayloads {
    private EntityEventPayloads() {
    }

    public static @NotNull AttackEntityPost attackPost(@NotNull RPlayer player, @NotNull REntity entity, boolean cancelled) {
        return new AttackEntityPost(Objects.requireNonNull(player, "player"), Objects.requireNonNull(entity, "entity"), cancelled);
    }

    public static @NotNull InteractEntityPost interactPost(@NotNull RPlayer player, @NotNull REntity entity, boolean cancelled) {
        return new InteractEntityPost(Objects.requireNonNull(player, "player"), Objects.requireNonNull(entity, "entity"), cancelled);
    }

    public static @NotNull EntitySpawnPost spawnPost(@NotNull REntity entity, @NotNull String reason, boolean cancelled) {
        return new EntitySpawnPost(Objects.requireNonNull(entity, "entity"), Objects.requireNonNull(reason, "reason"), cancelled);
    }

    public static @NotNull EntitySpawnSnapshot spawnSnapshot(@NotNull REntity entity, @NotNull String reason, boolean cancelled) {
        return EntitySpawnSnapshot.capture(Objects.requireNonNull(entity, "entity"), Objects.requireNonNull(reason, "reason"), cancelled);
    }

    public static @NotNull EntityHurtPost hurtPost(@NotNull REntity entity, @NotNull String damageTypeKey, boolean cancelled) {
        return new EntityHurtPost(Objects.requireNonNull(entity, "entity"), Objects.requireNonNull(damageTypeKey, "damageTypeKey"), cancelled);
    }

    public static @NotNull EntityHurtSnapshot hurtSnapshot(@NotNull REntity entity, @NotNull String damageTypeKey, boolean cancelled) {
        return EntityHurtSnapshot.capture(Objects.requireNonNull(entity, "entity"), Objects.requireNonNull(damageTypeKey, "damageTypeKey"), cancelled);
    }
}
