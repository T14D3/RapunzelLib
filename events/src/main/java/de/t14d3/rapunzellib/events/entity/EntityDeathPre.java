package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Pre-event fired before an entity dies.
 *
 * <p>This event is cancellable. Denying it prevents the death when the dying
 * entity is a player: the Paper bridge maps a deny to
 * {@code PlayerDeathEvent#setCancelled(boolean)}.</p>
 *
 * <p><strong>Platform caveat (Paper):</strong> for non-player deaths the
 * denial <em>cannot</em> be honored - Bukkit's {@code EntityDeathEvent} is
 * not cancellable. The bridge still dispatches this event for mobs (so
 * consumers can observe the imminent death and read the payload), but a deny
 * has no effect there; Paper degrades gracefully and the event remains
 * fire-and-forget for non-player entities. This is a platform-generic API
 * limitation, not a bug in this module.</p>
 *
 * <p>Player-only field: {@link #deathMessage()} is only populated when the
 * dying entity is a player (the death message shown in chat); it is empty for
 * non-player deaths.</p>
 */
public final class EntityDeathPre extends BaseCancellablePreEvent {
    private final REntity entity;
    private final REntitySnapshot snapshot;
    private final REntity killer;
    private final String cause;
    private final RLocation position;
    private final Component deathMessage;

    public EntityDeathPre(
        REntity entity,
        @Nullable REntity killer,
        String cause,
        RLocation position,
        @Nullable Component deathMessage
    ) {
        this(entity, killer, cause, position, deathMessage, false);
    }

    public EntityDeathPre(
        REntity entity,
        @Nullable REntity killer,
        String cause,
        RLocation position,
        @Nullable Component deathMessage,
        boolean isCancelled
    ) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        this.killer = killer;
        this.cause = Objects.requireNonNull(cause, "cause");
        this.position = Objects.requireNonNull(position, "position");
        this.deathMessage = deathMessage;
        setCancelled(isCancelled);
    }

    /**
     * Returns the dying entity.
     *
     * @return the entity
     */
    public REntity entity() {
        return entity;
    }

    /**
     * Returns the killer, if the death source was caused by an entity.
     *
     * @return the killer entity, or empty when the death had no entity cause
     */
    public Optional<REntity> killer() {
        return Optional.ofNullable(killer);
    }

    /**
     * Returns the death cause name (the damage type key, e.g.
     * "minecraft:player_attack").
     *
     * @return the cause name
     */
    public String cause() {
        return cause;
    }

    /**
     * Returns the position of the death.
     *
     * @return the death location
     */
    public RLocation position() {
        return position;
    }

    /**
     * Returns the player-only death message (the message shown in chat), if
     * the dying entity is a player.
     *
     * @return the death message, or empty for non-player deaths
     */
    public Optional<Component> deathMessage() {
        return Optional.ofNullable(deathMessage);
    }

    /**
     * Returns a snapshot of the entity's state at death time.
     */
    public REntitySnapshot snapshot() {
        return snapshot;
    }
}
