package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

/**
 * Listener for bot events.
 * <p>
 * Implementations receive {@link BotEvent} notifications when bots produce
 * output or change state. Used by the event-driven bot communication system
 * to decouple event producers (e.g., console callbacks) from consumers
 * (e.g., test assertions waiting for specific events).
 * </p>
 */
@FunctionalInterface
public interface BotEventListener {

    /**
     * Called when a bot event occurs.
     *
     * @param event the bot event
     */
    void onBotEvent(@NotNull BotEvent event);

    /**
     * A bot event representing a state change or output from a bot.
     *
     * @param type    the event type (e.g., "CHAT", "POSITION", "HEALTH", "READY", "ERROR")
     * @param botName the name of the bot that produced the event
     * @param message the event message or payload
     * @param server  the logical server the event was produced on; empty when
     *                the producer did not tag the event with a server
     */
    record BotEvent(@NotNull String type, @NotNull String botName, @NotNull String message, @NotNull String server) {

        /**
         * Legacy constructor for producers without server context.
         * The event is tagged with an empty server.
         */
        BotEvent(@NotNull String type, @NotNull String botName, @NotNull String message) {
            this(type, botName, message, "");
        }
    }
}
