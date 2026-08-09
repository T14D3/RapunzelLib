package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.REntityType;

import java.util.Objects;
import java.util.UUID;

/**
 * Pre-event fired before an entity (including players) teleports.
 *
 * <p>This event is cancellable. If denied, the teleport is cancelled. Carries
 * the entity, the source and target locations, and the teleport cause.</p>
 *
 * <p>The Paper bridge dispatches this from both {@code EntityTeleportEvent}
 * and {@code PlayerTeleportEvent} (which does not extend the former on
 * current Paper, so both must be bridged - the same asymmetry the
 * {@link EntityTeleportPost} bridge documents). The cause is only available
 * for player teleports; generic entity teleports and platforms without a
 * cause concept dispatch {@link EntityTeleportCause#UNKNOWN}.</p>
 */
public final class EntityTeleportPre extends BaseCancellablePreEvent {
    private final REntity entity;
    private final RLocation from;
    private final RLocation to;
    private final EntityTeleportCause cause;

    public EntityTeleportPre(
        REntity entity,
        RLocation from,
        RLocation to,
        EntityTeleportCause cause
    ) {
        this(entity, from, to, cause, false);
    }

    public EntityTeleportPre(
        REntity entity,
        RLocation from,
        RLocation to,
        EntityTeleportCause cause,
        boolean isCancelled
    ) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.cause = Objects.requireNonNull(cause, "cause");
        setCancelled(isCancelled);
    }

    /**
     * Returns the entity that is about to teleport.
     *
     * @return the entity
     */
    public REntity entity() {
        return entity;
    }

    /**
     * Returns the UUID of the entity that is about to teleport.
     *
     * @return the entity UUID
     */
    public UUID uuid() {
        return entity.uuid();
    }

    /**
     * Returns the entity type key.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return entity.typeKey();
    }

    /**
     * Returns the typed entity type wrapper, resolved from the live entity.
     *
     * @return the entity type
     */
    public REntityType entityType() {
        return entity.requireType();
    }

    /**
     * Returns the location the entity is teleporting from.
     *
     * @return the source location
     */
    public RLocation from() {
        return from;
    }

    /**
     * Returns the location the entity is teleporting to.
     *
     * @return the target location
     */
    public RLocation to() {
        return to;
    }

    /**
     * Returns the cause of this teleport.
     *
     * @return the teleport cause
     */
    public EntityTeleportCause cause() {
        return cause;
    }

    /**
     * Convenience for {@code cause() == EntityTeleportCause.PLUGIN}.
     *
     * @return true when the teleport originates from plugin code
     */
    public boolean isPluginTeleport() {
        return cause == EntityTeleportCause.PLUGIN;
    }
}
