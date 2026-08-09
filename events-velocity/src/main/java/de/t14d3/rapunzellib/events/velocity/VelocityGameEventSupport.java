package de.t14d3.rapunzellib.events.velocity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;

/**
 * GameEvent support manifest for the Velocity proxy platform.
 *
 * <p>The proxy bridges the connection lifecycle events
 * ({@link PlayerJoinPost} from {@code PostLoginEvent},
 * {@link PlayerQuitPost} from {@code DisconnectEvent}); all remaining
 * catalog events are game-server events without a proxy equivalent and stay
 * unsupported here (the {@link GameEventSupportManifest.Builder} accounts
 * for the full catalog automatically).</p>
 */
final class VelocityGameEventSupport {
    static final GameEventSupportManifest MANIFEST = GameEventSupportManifest.builder(PlatformId.VELOCITY)
        .nativeSupport(
            "Velocity proxy event bridge",
            PlayerJoinPost.class,
            PlayerQuitPost.class
        )
        .build();

    private VelocityGameEventSupport() {
    }
}
