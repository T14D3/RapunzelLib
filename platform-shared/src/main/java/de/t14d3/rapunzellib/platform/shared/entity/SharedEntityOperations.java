package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryHandles;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

public final class SharedEntityOperations {
    private SharedEntityOperations() {
    }

    public static boolean teleport(@NotNull Entity entity, @NotNull RLocation location, @NotNull SharedWorldHooks worldHooks) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(worldHooks, "worldHooks");
        SharedLocationSemantics.requireFinite(location);

        ServerLevel currentLevel = (ServerLevel) entity.level();
        MinecraftServer server = currentLevel.getServer();
        ServerLevel targetLevel = worldHooks.resolveWorld(server, location.world()).orElse(currentLevel);
        EnumSet<Relative> relatives = EnumSet.noneOf(Relative.class);

        boolean moved = entity.teleportTo(targetLevel, location.x(), location.y(), location.z(), relatives, location.yaw(), location.pitch(), true);
        if (moved) {
            SharedLocationSemantics.apply(entity, location);
        }
        return moved;
    }

    public static boolean damage(@NotNull LivingEntity entity, double amount) {
        Objects.requireNonNull(entity, "entity");
        requireFiniteNonNegative(amount, "amount");
        if (amount == 0.0d) {
            return true;
        }
        float before = entity.getHealth();
        entity.hurt(entity.damageSources().generic(), (float) amount);
        return entity.getHealth() < before || !entity.isAlive();
    }

    public static boolean heal(@NotNull LivingEntity entity, double amount) {
        Objects.requireNonNull(entity, "entity");
        requireFiniteNonNegative(amount, "amount");
        if (amount == 0.0d) {
            return true;
        }
        entity.heal((float) amount);
        return true;
    }

    public static @NotNull Optional<REntity> spawn(
        @NotNull ServerLevel level,
        @NotNull RRegistryRef<REntityType> type,
        @NotNull RLocation location,
        @NotNull SharedWorldHooks worldHooks
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(worldHooks, "worldHooks");
        SharedLocationSemantics.requireFinite(location);

        MinecraftServer server = level.getServer();
        ServerLevel targetLevel = worldHooks.resolveWorld(server, location.world()).orElse(level);

        EntityType<?> entityType = RRegistryHandles.find(type, EntityType.class)
            .orElseGet(() -> BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(type.key().asString())));
        if (entityType == null) {
            return Optional.empty();
        }

        Entity entity = entityType.create(targetLevel, EntitySpawnReason.COMMAND);
        if (entity == null) {
            return Optional.empty();
        }

        SharedLocationSemantics.apply(entity, location);
        if (!targetLevel.addFreshEntity(entity)) {
            return Optional.empty();
        }
        return Rapunzel.context().entities().wrap(entity);
    }

    private static void requireFiniteNonNegative(double amount, @NotNull String name) {
        if (!Double.isFinite(amount) || amount < 0.0d) {
            throw new IllegalArgumentException(name + " must be a finite non-negative number");
        }
    }
}
