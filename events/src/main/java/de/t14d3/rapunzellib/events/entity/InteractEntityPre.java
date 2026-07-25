package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import de.t14d3.rapunzellib.registry.REntityType;

import java.util.Objects;

/**
 * Pre-event fired before a player interacts with an entity.
 *
 * <p>This event is cancellable. If denied, the interaction will not occur.
 * Contains the player, the target entity, and a snapshot of the entity's state.</p>
 */
public final class InteractEntityPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final REntity entity;
    private final REntitySnapshot snapshot;

    public InteractEntityPre(RPlayer player, REntity entity) {
        this(player, entity, false);
    }

    public InteractEntityPre(RPlayer player, REntity entity, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        setCancelled(isCancelled);
    }

    public RPlayer player() {
        return player;
    }

    public REntity entity() {
        return entity;
    }

    public java.util.Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }

    public REntitySnapshot snapshot() {
        return snapshot;
    }

    public RWorldRef world() {
        return snapshot.world();
    }

    public RBlockPos pos() {
        return snapshot.pos();
    }

    public RKey entityTypeKey() {
        return snapshot.entityTypeKey();
    }

    /**
     * Returns the typed entity type wrapper, resolved from the live entity.
     *
     * @return the entity type
     */
    public REntityType entityType() {
        return entity.requireType();
    }
}
