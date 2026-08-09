package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Post-event fired after an entity has died.
 *
 * <p>Carries the same payload as {@link EntityDeathPre}: the entity, the
 * killer, the death cause name, the death position, and the player-only death
 * message. See {@link EntityDeathPre} for the platform caveat about
 * non-player death cancellation.</p>
 *
 * @param entity        the entity that died
 * @param snapshot      a snapshot of the entity's state
 * @param killer        the killer entity, or {@code null} when the death had no entity cause
 * @param cause         the death cause name (damage type key)
 * @param position      the death position
 * @param deathMessage  the player-only death message, or {@code null} for non-player deaths
 * @param cancelled     whether the death was cancelled
 */
public record EntityDeathPost(
    REntity entity,
    REntitySnapshot snapshot,
    @Nullable REntity killer,
    String cause,
    RLocation position,
    @Nullable Component deathMessage,
    boolean cancelled
) implements GamePostEvent {

    /**
     * Creates an EntityDeathPost from an entity, killer, cause, position, and
     * cancelled state (without a death message).
     */
    public EntityDeathPost(
        REntity entity,
        @Nullable REntity killer,
        String cause,
        RLocation position,
        boolean cancelled
    ) {
        this(entity, entity.snapshot(), killer, cause, position, null, cancelled);
    }

    /**
     * Creates an EntityDeathPost from an entity, killer, cause, position,
     * player-only death message, and cancelled state.
     */
    public EntityDeathPost(
        REntity entity,
        @Nullable REntity killer,
        String cause,
        RLocation position,
        @Nullable Component deathMessage,
        boolean cancelled
    ) {
        this(entity, entity.snapshot(), killer, cause, position, deathMessage, cancelled);
    }

    /**
     * Returns the killer, if the death source was caused by an entity.
     *
     * @return the killer entity, or empty when the death had no entity cause
     */
    public Optional<REntity> killerIfPresent() {
        return Optional.ofNullable(killer);
    }

    /**
     * Returns the player-only death message (the message shown in chat), if
     * the dying entity is a player.
     *
     * @return the death message, or empty for non-player deaths
     */
    public Optional<Component> deathMessageIfPresent() {
        return Optional.ofNullable(deathMessage);
    }
}
