package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Objects;

/**
 * Pre-event fired before a player sends a chat message or runs a command.
 *
 * <p>This event is cancellable. If denied, the message or command is
 * suppressed.</p>
 *
 * <p><strong>Threading note:</strong> chat messages fire on the server's
 * <em>async chat thread</em> (the Paper bridge dispatches from
 * {@code AsyncChatEvent}), while commands fire synchronously on the main
 * server thread ({@code PlayerCommandPreprocessEvent}). Consumers that touch
 * the world or player state must hop back onto the main thread via the
 * {@link de.t14d3.rapunzellib.scheduler.Scheduler scheduler} for chat
 * messages.</p>
 *
 * <p>{@link #isCommand()} distinguishes the two sources. For commands,
 * {@link #content()} includes the leading {@code '/'}.</p>
 */
public final class PlayerMessagePre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final String content;
    private final boolean isCommand;

    public PlayerMessagePre(RPlayer player, String content, boolean isCommand) {
        this(player, content, isCommand, false);
    }

    public PlayerMessagePre(RPlayer player, String content, boolean isCommand, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.content = Objects.requireNonNull(content, "content");
        this.isCommand = isCommand;
        setCancelled(isCancelled);
    }

    public RPlayer player() {
        return player;
    }

    /**
     * Returns the message content. For commands this includes the leading
     * {@code '/'}.
     *
     * @return the message content
     */
    public String content() {
        return content;
    }

    /**
     * Whether this is a command ({@code true}) or a chat message
     * ({@code false}).
     *
     * @return the source kind
     */
    public boolean isCommand() {
        return isCommand;
    }
}
