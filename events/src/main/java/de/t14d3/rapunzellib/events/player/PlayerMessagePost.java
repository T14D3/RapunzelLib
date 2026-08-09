package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Objects;

/**
 * Post-event fired after a player sent a chat message or ran a command.
 *
 * <p>Carries the same payload as {@link PlayerMessagePre}. See that class for
 * the async/sync threading note.</p>
 *
 * @param player    the sending player
 * @param content   the message content (commands include the leading '/')
 * @param isCommand whether this was a command rather than a chat message
 * @param cancelled whether the message/command was cancelled
 */
public record PlayerMessagePost(RPlayer player, String content, boolean isCommand, boolean cancelled) implements GamePostEvent {

    public PlayerMessagePost {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(content, "content");
    }
}
