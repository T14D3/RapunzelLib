package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RGameMode;
import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Objects;
import java.util.Set;

/**
 * Post-event fired when a player's state changes (gamemode, sneak, fly,
 * sprint, or riding).
 *
 * <p>The bridge fires one update per source event and marks the respective
 * {@link StateField} as changed. {@link #snapshot()} always carries the full
 * current state, so consumers can read the new value of the changed field
 * from the snapshot and compare against the other fields.</p>
 *
 * @param player        the player whose state changed
 * @param snapshot      the full current player state
 * @param changedFields the fields that changed in this update
 */
public record PlayerStatePost(RPlayer player, PlayerStateSnapshot snapshot, Set<StateField> changedFields) implements GamePostEvent {

    public PlayerStatePost {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(changedFields, "changedFields");
        changedFields = Set.copyOf(changedFields);
    }

    /**
     * The player state fields tracked by {@link PlayerStatePost}.
     */
    public enum StateField {
        GAMEMODE,
        SNEAKING,
        FLYING,
        SPRINTING,
        RIDING,
    }

    /**
     * Immutable snapshot of a player's state.
     *
     * @param gamemode  the gamemode
     * @param sneaking  whether the player is sneaking
     * @param flying    whether the player is flying
     * @param sprinting whether the player is sprinting
     * @param riding    whether the player is riding a vehicle/entity
     */
    public record PlayerStateSnapshot(
        RGameMode gamemode,
        boolean sneaking,
        boolean flying,
        boolean sprinting,
        boolean riding
    ) {
        public PlayerStateSnapshot {
            Objects.requireNonNull(gamemode, "gamemode");
        }
    }
}
