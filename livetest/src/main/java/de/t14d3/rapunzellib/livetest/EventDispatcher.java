package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Single-thread-safe fan-out for {@link BotEventListener.BotEvent}s.
 *
 * <p>The dispatcher keeps a small set of registered {@link BotEventListener
 * listeners} plus a set of <em>pending awaiters</em> - one-shot
 * {@link java.util.concurrent.CompletableFuture}s gated by a predicate and a
 * timeout. When an event arrives, the dispatcher:</p>
 *
 * <ol>
 *   <li>snapshots the listeners and the awaiters (one volatile read each),</li>
 *   <li>fires each listener, swallowing exceptions so one bad listener can't
 *       starve the rest,</li>
 *   <li>evaluates each awaiter's predicate; on a match the future completes
 *       and the awaiter is removed,</li>
 *   <li>evaluates each awaiter's deadline; on expiry the future completes
 *       exceptionally and the awaiter is removed.</li>
 * </ol>
 *
 * <p>The implementation is intentionally single-class to keep the hot path
 * lock-free. Calls to {@link #dispatch} come from one thread (the TCP reader
 * loop); calls to {@link #registerAwait} and {@link #addListener} may come
 * from any thread.</p>
 */
final class EventDispatcher {

    private final List<BotEventListener> listeners = new CopyOnWriteArrayList<>();
    private final java.util.Set<AwaitSlot> awaitSlots = ConcurrentHashMap.newKeySet();

    /**
     * Adds a one-shot awaiter that completes when an event matching the
     * predicate arrives, or when the deadline elapses - whichever happens
     * first. The caller is responsible for wiring the timeout onto the
     * returned future (e.g. via {@code orTimeout}) and for removing the slot
     * on completion via {@link #removeAwait}.
     *
     * @param slot the await slot; created by the caller
     */
    void registerAwait(@NotNull AwaitSlot slot) {
        awaitSlots.add(slot);
    }

    /** Removes a previously registered await slot. No-op if already gone. */
    void removeAwait(@NotNull AwaitSlot slot) {
        awaitSlots.remove(slot);
    }

    void addListener(@NotNull BotEventListener listener) {
        listeners.add(listener);
    }

    void removeListener(@NotNull BotEventListener listener) {
        listeners.remove(listener);
    }

    /** Dispatches a single event. */
    void dispatch(@NotNull BotEventListener.BotEvent event) {
        for (BotEventListener l : listeners) {
            try {
                l.onBotEvent(event);
            } catch (Exception ignored) {
                // Listener fault MUST NOT affect other listeners.
            }
        }
        for (AwaitSlot slot : awaitSlots) {
            try {
                if (slot.future.isDone()) {
                    awaitSlots.remove(slot);
                    continue;
                }
                if (slot.predicate.test(event)) {
                    slot.future.complete(event);
                    awaitSlots.remove(slot);
                }
            } catch (Exception ignored) {
                // Awaiter predicate fault MUST NOT affect other awaiters.
            }
        }
    }

    /** Removes and exceptional-completes all awaiters - e.g. on transport drop. */
    void failAll(@NotNull Throwable cause) {
        for (AwaitSlot slot : awaitSlots) {
            slot.future.completeExceptionally(cause);
        }
        awaitSlots.clear();
    }

    /** One-shot registration for a single awaited event. */
    static final class AwaitSlot {
        final CompletableFuture<BotEventListener.BotEvent> future;
        final Predicate<BotEventListener.BotEvent> predicate;

        AwaitSlot(@NotNull Predicate<BotEventListener.BotEvent> predicate) {
            this.future = new CompletableFuture<>();
            this.predicate = predicate;
        }
    }
}
