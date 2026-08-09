package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pre-event fired before a player logs in, covering ban checks and
 * maintenance gates in one place.
 *
 * <p>This event is cancellable. If denied, the player is kicked; the optional
 * deny {@link Component reason} is used as the kick message.</p>
 *
 * <p><strong>Threading note:</strong> this event fires on the server's
 * <em>async login thread</em> (the Paper bridge dispatches from
 * {@code AsyncPlayerPreLoginEvent}). The player has not joined yet, so the
 * payload carries the player's {@link #name()} and {@link #uuid()} rather
 * than a live player reference. Consumers must not touch world/player state
 * from this thread.</p>
 */
public final class PlayerLoginPre extends BaseCancellablePreEvent {
    private final String name;
    private final UUID uuid;

    public PlayerLoginPre(String name, @Nullable UUID uuid) {
        this(name, uuid, false);
    }

    public PlayerLoginPre(String name, @Nullable UUID uuid, boolean isCancelled) {
        this.name = Objects.requireNonNull(name, "name");
        this.uuid = uuid;
        setCancelled(isCancelled);
    }

    /**
     * Returns the player's name.
     *
     * @return the player name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the player's UUID, when known.
     *
     * <p>The platform may not resolve the UUID for every login attempt (e.g.
     * when the connection is refused before lookup); the Paper bridge passes
     * the value through as-is.</p>
     *
     * @return the UUID, or empty when unknown
     */
    public Optional<UUID> uuid() {
        return Optional.ofNullable(uuid);
    }
}
