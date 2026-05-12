package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Pre-event fired before an entity spawns in the world.
 *
 * <p>This event is cancellable. If denied, the entity will not spawn.
 * Contains the world, position, entity type, and spawn reason.</p>
 */
public final class EntitySpawnPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey entityTypeKey;
    private final String reason;

    /**
     * Creates a new EntitySpawnPre event.
     *
     * @param world         the world reference
     * @param pos           the spawn position
     * @param entityTypeKey the entity type key
     * @param reason        the spawn reason
     */
    public EntitySpawnPre(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String reason) {
        this(world, pos, entityTypeKey, reason, false);
    }

    /**
     * Creates a new EntitySpawnPre event with cancelled state.
     *
     * @param world         the world reference
     * @param pos           the spawn position
     * @param entityTypeKey the entity type key
     * @param reason        the spawn reason
     * @param isCancelled   whether the event is initially cancelled
     */
    public EntitySpawnPre(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String reason, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        this.reason = Objects.requireNonNull(reason, "reason");
        setCancelled(isCancelled);
    }

    /**
     * Returns the world where the entity will spawn.
     *
     * @return the world
     */
    public RWorldRef world() {
        return world;
    }

    /**
     * Returns the spawn position.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return pos;
    }

    /**
     * Returns the entity type key.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return entityTypeKey;
    }

    /**
     * Returns the spawn reason (e.g., "natural", "spawn_egg", "command").
     *
     * @return the spawn reason
     */
    public String reason() {
        return reason;
    }
}
