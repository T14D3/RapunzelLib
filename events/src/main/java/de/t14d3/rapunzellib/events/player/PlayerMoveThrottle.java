package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Library-scope configuration + throttling for {@link PlayerMovePre} /
 * {@link PlayerMovePost} dispatch, shared by every platform bridge and the
 * {@code PlayerMoveMixin}.
 *
 * <p>Config section {@code events.player.move} in the library config file
 * ({@code config.yml} in the platform data directory, merged with the
 * bundled default resource):</p>
 * <ul>
 *   <li>{@code enabled} (default {@code true}) - master toggle; when
 *       {@code false} no player-move events are dispatched at all.</li>
 *   <li>{@code min-distance} (default {@code 0.5}) - minimum displacement
 *       (blocks, Euclidean) since the last dispatched move event. Moves
 *       shorter than this are suppressed (head rotation, micro-steps).</li>
 *   <li>{@code max-rate-ms} (default {@code 250}) - minimum interval between
 *       dispatched move events per player; {@code 0} disables the rate limit.</li>
 * </ul>
 *
 * <p>Values are read once at bridge install time and cached; the config file
 * is NOT re-read per event. The throttle is a pure gate: platform bridges and
 * the mixin must still pair pre/post dispatch themselves (a post is only
 * dispatched for an event whose pre passed the throttle).</p>
 */
public final class PlayerMoveThrottle {
    private static final String SECTION = "events.player.move";

    private static volatile boolean enabled = true;
    private static volatile double minDistance = 0.5D;
    private static volatile long maxRateMs = 250L;

    /** Per-player last accepted move anchor (position + time of last dispatch). */
    private static final Map<UUID, Anchor> LAST = new ConcurrentHashMap<>();

    private PlayerMoveThrottle() {
    }

    /**
     * Loads the {@code events.player.move} section from the library config
     * file ({@code config.yml} in {@link RapunzelContext#dataDirectory()}),
     * creating it with the bundled defaults on first run.
     *
     * @param context the Rapunzel context (config service + data directory)
     */
    public static void load(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");
        YamlConfig config = context.configs().load(
            context.dataDirectory().resolve("config.yml"),
            "config.yml"
        );
        configure(
            config.getBoolean(SECTION + ".enabled", true),
            config.getDouble(SECTION + ".min-distance", 0.5D),
            config.getLong(SECTION + ".max-rate-ms", 250L)
        );
    }

    /**
     * Applies the throttle settings directly (used by tests / programmatic setup).
     */
    public static void configure(boolean moveEnabled, double moveMinDistance, long moveMaxRateMs) {
        enabled = moveEnabled;
        minDistance = moveMinDistance;
        maxRateMs = moveMaxRateMs;
        LAST.clear();
    }

    /**
     * Whether a move event for {@code player} (from {@code from} to {@code to})
     * should be dispatched.
     *
     * <p>Returns {@code false} when the move section is disabled, when the
     * player has not displaced at least {@code min-distance} since the last
     * dispatched move, or when the {@code max-rate-ms} interval has not
     * elapsed yet. Accepted moves re-anchor the player's position/time; the
     * {@code to} position becomes the new anchor.</p>
     *
     * @param uuid the moving player's UUID
     * @param from the position the player is moving from
     * @param to   the position the player is moving to
     * @return {@code true} when the event should be dispatched
     */
    public static boolean shouldDispatch(@NotNull UUID uuid, @NotNull RLocation from, @NotNull RLocation to) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!enabled) return false;

        long now = System.currentTimeMillis();
        Anchor anchor = LAST.get(uuid);
        if (anchor == null) {
            LAST.put(uuid, new Anchor(to.x(), to.y(), to.z(), now));
            return true;
        }

        double dx = to.x() - anchor.x;
        double dy = to.y() - anchor.y;
        double dz = to.z() - anchor.z;
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) < minDistance) {
            return false;
        }
        if (maxRateMs > 0 && now - anchor.time < maxRateMs) {
            return false;
        }

        anchor.update(to.x(), to.y(), to.z(), now);
        return true;
    }

    /**
     * Whether the most recent accepted move for {@code player} was exactly
     * this {@code from}/{@code to} pair (within a short window). Platform
     * bridges use this in their post handler to dispatch a post only for the
     * move whose pre was throttled through, keeping pre/post paired.
     *
     * @param uuid the moving player's UUID
     * @param from the position the player moved from
     * @param to   the position the player moved to
     * @return {@code true} when this exact move was accepted by the pre handler
     */
    public static boolean wasAccepted(@NotNull UUID uuid, @NotNull RLocation from, @NotNull RLocation to) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!enabled) return false;

        Anchor anchor = LAST.get(uuid);
        if (anchor == null) return false;
        // The accepted anchor is the event's "to" position; require an exact
        // match within a short window so a later, coincidentally identical
        // move does not pair with an older accepted one.
        return Math.abs(anchor.time - System.currentTimeMillis()) < 100L
            && anchor.x == to.x() && anchor.y == to.y() && anchor.z == to.z();
    }

    /** Clears per-player throttle state (bridge shutdown). */
    public static void reset() {
        LAST.clear();
    }

    private static final class Anchor {
        private double x;
        private double y;
        private double z;
        private long time;

        Anchor(double x, double y, double z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }

        void update(double x, double y, double z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }
}
