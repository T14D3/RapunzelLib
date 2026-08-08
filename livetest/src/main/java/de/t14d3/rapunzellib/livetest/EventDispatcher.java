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
 * <p><em>Replay buffer:</em> events are inherently asynchronous - a bot's
 * reply can be broadcast by the server and dispatched on the reader thread
 * while the test thread is still awaiting an <em>earlier</em> event (e.g. in a
 * two-bot exchange, Bob's notification arrives in the same burst as Alice's
 * confirmation, before the test has registered Bob's await slot). Awaiter
 * registration and event dispatch therefore race: without a fallback, any
 * event dispatched before its slot is registered is lost and the await times
 * out even though the underlying behavior worked. To close that race, the
 * dispatcher keeps a small bounded replay buffer of recently dispatched
 * events; {@link #registerAwait} first evaluates the slot's predicate against
 * the buffered events (newest first) and completes immediately on a match.
 * A buffered event satisfies at most one slot - once matched it is consumed
 * so it cannot be replayed to slots registered later.</p>
 *
 * <p><em>Freshness bound:</em> the race the replay buffer closes is
 * millisecond-scale (an event broadcast while the test thread is still
 * registering its slot). An event that was dispatched long before a slot was
 * registered is <em>stale</em> - it must not satisfy the await (e.g. an old
 * cross-server channel message sitting unconsumed in the buffer would match a
 * later {@code awaitAnyChat}'s fallback text and return the wrong payload).
 * Each buffered event therefore records the time it was dispatched, and the
 * replay scan ignores events older than {@link #REPLAY_MAX_AGE_MS}.</p>
 *
 * <p>Calls to {@link #dispatch} come from one thread (the TCP reader loop);
 * calls to {@link #registerAwait} and {@link #addListener} may come from any
 * thread. The replay buffer is guarded by a small lock; the await-slot and
 * listener sets are lock-free (concurrent sets).</p>
 */
final class EventDispatcher {

    /** Upper bound on the number of events kept for late awaiter replays. */
    private static final int REPLAY_BUFFER_CAPACITY = 512;

    /**
     * Upper bound on the age of a buffered event that may still be replayed to
     * a late-registered await slot. The dispatch-vs-registration race is
     * millisecond-scale, so 5s is a generous margin; anything older is stale
     * and must not satisfy a slot registered later.
     */
    private static final long REPLAY_MAX_AGE_MS = 5_000L;

    private final List<BotEventListener> listeners = new CopyOnWriteArrayList<>();
    private final java.util.Set<AwaitSlot> awaitSlots = ConcurrentHashMap.newKeySet();

    /**
     * Recently dispatched events, oldest first, each with the wall-clock time
     * it was dispatched. Guarded by {@link #replayLock}; written by the
     * dispatch thread and read by {@link #registerAwait} from any thread.
     */
    private final java.util.ArrayDeque<BufferedEvent> replayBuffer = new java.util.ArrayDeque<>();
    private final Object replayLock = new Object();

    /** A dispatched event together with the time it entered the replay buffer. */
    private record BufferedEvent(BotEventListener.BotEvent event, long dispatchedAt) {}

    /**
     * Adds a one-shot awaiter that completes when an event matching the
     * predicate arrives, or when the deadline elapses - whichever happens
     * first. The caller is responsible for wiring the timeout onto the
     * returned future (e.g. via {@code orTimeout}) and for removing the slot
     * on completion via {@link #removeAwait}.
     *
     * <p>The slot is registered <em>before</em> the replay buffer is scanned
     * and the buffered events are evaluated <em>newest first</em>, so a slot
     * registered after its matching event was already dispatched still
     * completes (and the matched event is consumed so it cannot satisfy a
     * later slot as well).</p>
     *
     * @param slot the await slot; created by the caller
     */
    void registerAwait(@NotNull AwaitSlot slot) {
        awaitSlots.add(slot);
        long now = System.currentTimeMillis();
        synchronized (replayLock) {
            java.util.Iterator<BufferedEvent> it = replayBuffer.descendingIterator();
            while (it.hasNext()) {
                BufferedEvent buffered = it.next();
                if (now - buffered.dispatchedAt() > REPLAY_MAX_AGE_MS) {
                    // Stale - must not satisfy a slot registered this late.
                    continue;
                }
                try {
                    if (slot.predicate.test(buffered.event())) {
                        slot.future.complete(buffered.event());
                        it.remove(); // consume - one-shot events match at most one slot
                        break;
                    }
                } catch (Exception ignored) {
                    // Awaiter predicate fault MUST NOT affect other awaiters.
                }
            }
        }
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
        // Buffer the event *before* evaluating the slots so an await slot
        // registered concurrently (from another thread) can still replay it -
        // the ordering guarantees every event is either matched during this
        // dispatch or visible to a subsequent registerAwait() replay.
        BufferedEvent buffered = new BufferedEvent(event, System.currentTimeMillis());
        boolean matched = false;
        synchronized (replayLock) {
            replayBuffer.addLast(buffered);
            while (replayBuffer.size() > REPLAY_BUFFER_CAPACITY) {
                replayBuffer.removeFirst();
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
                    matched = true;
                }
            } catch (Exception ignored) {
                // Awaiter predicate fault MUST NOT affect other awaiters.
            }
        }
        if (matched) {
            // The event satisfied a slot - consume it so it cannot be replayed
            // to slots registered later (one-shot semantics).
            synchronized (replayLock) {
                replayBuffer.removeLastOccurrence(buffered);
            }
        }
    }

    /** Removes and exceptional-completes all awaiters - e.g. on transport drop. */
    void failAll(@NotNull Throwable cause) {
        for (AwaitSlot slot : awaitSlots) {
            slot.future.completeExceptionally(cause);
        }
        awaitSlots.clear();
        // Drop the replay buffer too: the transport is gone, so the buffered
        // events are stale and must not satisfy awaits after a reconnect.
        synchronized (replayLock) {
            replayBuffer.clear();
        }
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
