package de.t14d3.rapunzellib.events.velocity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.GameEventSupportManifest;
import de.t14d3.rapunzellib.events.GameEventSupportParity;
import de.t14d3.rapunzellib.events.player.PlayerJoinPost;
import de.t14d3.rapunzellib.events.player.PlayerMessagePost;
import de.t14d3.rapunzellib.events.player.PlayerMessagePre;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.player.PlayerStatePost;

/**
 * GameEvent support manifest for the Velocity proxy platform.
 *
 * <p>The proxy bridges the connection lifecycle events
 * ({@link PlayerJoinPost} from {@code PostLoginEvent},
 * {@link PlayerQuitPost} from {@code DisconnectEvent}) and the message events
 * ({@link PlayerMessagePre}/{@link PlayerMessagePost} from
 * {@code PlayerChatEvent} and {@code CommandExecuteEvent}). Proxy caveat: the
 * chat message is the raw client text seen by the proxy - it may be unsigned
 * (the payload carries no signature). All remaining catalog events are
 * game-server events without a proxy equivalent and stay unsupported here
 * (the {@link GameEventSupportManifest.Builder} accounts for the full catalog
 * automatically).</p>
 */
final class VelocityGameEventSupport {
    static final GameEventSupportManifest MANIFEST = GameEventSupportManifest.builder(PlatformId.VELOCITY)
        .nativeSupport(
            "Velocity proxy event bridge",
            PlayerJoinPost.class,
            PlayerQuitPost.class
        )
        .nativeSupport(
            "Velocity PlayerChatEvent / CommandExecuteEvent bridge (proxy-level; chat message is the raw client text, may be unsigned)",
            PlayerMessagePre.class,
            PlayerMessagePost.class
        )
        .support(
            GameEventSupportParity.UNSUPPORTED,
            "Velocity is a proxy and cannot observe the backend player's game mode",
            PlayerStatePost.class
        )
        .build();

    private VelocityGameEventSupport() {
    }
}
