package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

/**
 * Utility for injecting bot events into the active {@link BotService}.
 * <p>
 * Bot instances are created via {@link Bot#connect(String, String)}.
 * This class provides the event-injection side of the bot system,
 * used internally by the console-based bot protocol.
 * </p>
 */
public final class BotFactory {

    // Lazily initialized console-based fallback service
    private static BotService consoleFallback;

    private BotFactory() {}

    /**
     * Returns the shared console-based fallback {@link BotService}.
     * Package-private so {@link Bot} can use it.
     */
    static synchronized @NotNull BotService newConsoleService() {
        if (consoleFallback == null) {
            consoleFallback = new SharedConsoleBotService();
        }
        return consoleFallback;
    }

    /**
     * Injects a bot event into the active bot service for processing.
     * <p>
     * Events are delivered to the service's event queue and dispatched to any
     * registered {@link BotEventListener}s.
     * </p>
     *
     * @param event the event to inject
     */
    public static void addEvent(@NotNull BotEvent event) {
        if (event != null) {
            BotService service = resolveService();
            service.injectEvent(event.type(), event.botName(), event.message());
        }
    }

    private static @NotNull BotService resolveService() {
        var ctx = de.t14d3.rapunzellib.Rapunzel.findContext();
        if (ctx.isPresent()) {
            var svc = ctx.get().services().find(BotService.class);
            if (svc.isPresent()) return svc.get();
        }
        return newConsoleService();
    }

    /**
     * A bot event record for the event queue.
     *
     * @param type    the event type
     * @param botName the bot name
     * @param message the event message
     */
    public record BotEvent(@NotNull String type, @NotNull String botName, @NotNull String message) {}
}
