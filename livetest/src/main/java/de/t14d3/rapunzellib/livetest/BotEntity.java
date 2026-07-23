package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable snapshot of a single entity tracked by a bot.
 *
 * <p>The fields are intentionally minimal: id, type id/name, position, rotation,
 * head rotation. Metadata decoding (pose, health, equipment) is a much bigger
 * surface and is intentionally left out of the first wire representation; test
 * authors who need it can iterate by querying the underlying bot.</p>
 *
 * <p>{@code typeName} follows Minecraft's vanilla convention (e.g. {@code "minecraft:zombie"},
 * {@code "minecraft:armor_stand"}). The bare name ({@code "zombie"}) is also
 * accepted as an alias by {@link #hasType(String)} for ergonomics.</p>
 */
public record BotEntity(int entityId,
                        int typeId,
                        @NotNull String typeName,
                        double x, double y, double z,
                        float yaw, float pitch,
                        float headYaw) {

    public BotEntity {
        Objects.requireNonNull(typeName, "typeName");
    }

    /**
     * @param name the type name to test (either the full {@code "minecraft:..." id} or the bare name)
     * @return {@code true} if this entity's type matches the given name in either form
     */
    public boolean hasType(@NotNull String name) {
        if (name.equalsIgnoreCase(typeName)) return true;
        String bare = typeName.startsWith("minecraft:")
                ? typeName.substring("minecraft:".length())
                : typeName;
        return name.equalsIgnoreCase(bare);
    }

    /**
     * @param ax,ay,az the target position
     * @return the Euclidean distance from this entity to the target
     */
    public double distanceTo(double ax, double ay, double az) {
        double dx = x - ax, dy = y - ay, dz = z - az;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** @return this entity's position as a {@link Bot.Position} (no head rotation preserved) */
    public @NotNull Bot.Position toPosition() {
        return new Bot.Position(x, y, z, yaw, pitch);
    }

    /** Sentinel used for "no such entity" returns. */
    public static @NotNull BotEntity unknown(int entityId) {
        return new BotEntity(entityId, -1, "minecraft:unknown", 0, 0, 0, 0, 0, 0);
    }

    /** Sentinel value returned by queries for "no entities found". */
    public static final BotEntity EMPTY = new BotEntity(-1, -1, "minecraft:unknown", 0, 0, 0, 0, 0, 0);

    /** @return {@code true} if this snapshot represents an absent entity */
    public boolean isUnknown() {
        return entityId < 0 || typeId < 0;
    }

    /** Convenience for tests that only care whether the entity exists. */
    public @NotNull Optional<BotEntity> asOptional() {
        return isUnknown() ? Optional.empty() : Optional.of(this);
    }
}
