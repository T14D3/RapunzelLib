package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RPlayer;
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
 * <p><strong>Threading note:</strong> the Paper bridge dispatches this from
 * {@code PlayerLoginEvent} on the server's <em>main thread</em>, where the
 * player object already exists. The payload therefore carries the live
 * {@link #player()} (permission checks against it are valid), plus the
 * player's {@link #name()} and {@link #uuid()}. Platforms without a player
 * object at login time (async pre-login hooks on other loaders) dispatch
 * with an absent player and must not touch world/player state from their
 * dispatch thread.</p>
 */
public final class PlayerLoginPre extends BaseCancellablePreEvent {
    private final String name;
    private final UUID uuid;
    private final RPlayer player;

    public PlayerLoginPre(String name, @Nullable UUID uuid) {
        this(name, uuid, null, false);
    }

    public PlayerLoginPre(String name, @Nullable UUID uuid, boolean isCancelled) {
        this(name, uuid, null, isCancelled);
    }

    public PlayerLoginPre(String name, @Nullable UUID uuid, @Nullable RPlayer player, boolean isCancelled) {
        this.name = Objects.requireNonNull(name, "name");
        this.uuid = uuid;
        this.player = player;
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

    /**
     * Returns the live player object, when the platform has one at login
     * time.
     *
     * <p>The Paper bridge dispatches from {@code PlayerLoginEvent}, where the
     * player exists and permissions are resolvable - permission-gated login
     * policies (e.g. maintenance mode) depend on this. Empty on platforms
     * whose login hook runs before the player object exists.</p>
     *
     * @return the player, or empty when not yet available
     */
    public Optional<RPlayer> player() {
        return Optional.ofNullable(player);
    }
}
