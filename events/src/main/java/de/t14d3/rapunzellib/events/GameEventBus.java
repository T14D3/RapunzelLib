package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Thread-safe event bus for dispatching game events to registered listeners.
 *
 * <p>This class is thread-safe and can be safely accessed from multiple threads
 * concurrently. All listener registration and event dispatch operations are
 * synchronized appropriately.</p>
 *
 * <p>Listeners can be registered for pre-events, post-events, and async snapshot events.
 * Each type is dispatched separately with appropriate threading guarantees.</p>
 */
@SuppressWarnings("UnusedReturnValue")
public final class GameEventBus implements AutoCloseable {
    /**
     * Subscription handle returned by listener registration methods.
     * Closing the subscription removes the registered listener.
     */
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final Scheduler scheduler;
    private final Logger logger;

    private final Map<Class<?>, ListenerList> preListeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, ListenerList> postListeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, ListenerList> asyncListeners = new ConcurrentHashMap<>();
    /**
     * Constructs a new game event bus.
     *
     * @param scheduler the scheduler for async dispatching
     * @param logger    the logger for error reporting
     */
    public GameEventBus(@NotNull Scheduler scheduler, @NotNull Logger logger) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Registers a pre-event listener.
     *
     * @param <E>        the event type
     * @param eventType  the event class to listen for
     * @param listener   the consumer to invoke
     * @return a subscription that can be closed to unregister
     */
    public <E extends CancellablePreEvent> @NotNull Subscription onPre(@NotNull Class<E> eventType, @NotNull Consumer<E> listener) {
        return register(preListeners, eventType, listener);
    }

    /**
     * Registers a post-event listener.
     *
     * @param <E>        the event type
     * @param eventType  the event class to listen for
     * @param listener   the consumer to invoke
     * @return a subscription that can be closed to unregister
     */
    public <E extends GamePostEvent> @NotNull Subscription onPost(@NotNull Class<E> eventType, @NotNull Consumer<E> listener) {
        return register(postListeners, eventType, listener);
    }

    /**
     * Registers an async snapshot listener.
     *
     * @param <E>        the event type
     * @param eventType  the event class to listen for
     * @param listener   the consumer to invoke on the async scheduler
     * @return a subscription that can be closed to unregister
     */
    public <E extends GameEventSnapshot> @NotNull Subscription onAsync(@NotNull Class<E> eventType, @NotNull Consumer<E> listener) {
        return register(asyncListeners, eventType, listener);
    }

    /**
     * Dispatches a pre-event to all registered listeners on the current thread.
     * Dispatching stops if the event is denied.
     *
     * @param <E>   the event type
     * @param event the event to dispatch
     */
    public <E extends CancellablePreEvent> void dispatchPre(@NotNull E event) {
        ListenerList list = preListeners.get(event.getClass());
        if (list == null) return;
        Consumer<?>[] listeners = list.snapshot();

        for (Consumer<?> listener : listeners) {
            if (event.isDenied()) return;
            dispatchUnchecked(listener, event);
        }
    }

    /**
     * Dispatches a post-event to all registered listeners on the current thread.
     *
     * @param <E>   the event type
     * @param event the event to dispatch
     */
    public <E extends GamePostEvent> void dispatchPost(@NotNull E event) {
        ListenerList list = postListeners.get(event.getClass());
        if (list == null) return;
        Consumer<?>[] listeners = list.snapshot();

        for (Consumer<?> listener : listeners) {
            dispatchUnchecked(listener, event);
        }
    }

    /**
     * Dispatches a snapshot event to all registered listeners on the async scheduler.
     *
     * @param <E>      the event type
     * @param snapshot the snapshot to dispatch
     */
    public <E extends GameEventSnapshot> void dispatchAsync(@NotNull E snapshot) {
        ListenerList list = asyncListeners.get(snapshot.getClass());
        if (list == null) return;
        Consumer<?>[] listeners = list.snapshot();

        scheduler.runAsync(() -> {
            for (Consumer<?> listener : listeners) {
                dispatchUnchecked(listener, snapshot);
            }
        });
    }

    /**
     * Dispatches entity event data asynchronously.
     * Creates an EntityEventData snapshot and dispatches it to registered listeners.
     *
     * @param playerUuid    the UUID of the player involved
     * @param world         the world reference
     * @param pos           the block position
     * @param entityTypeKey the entity type key
     * @param cancelled     whether the entity event was cancelled
     */
    public void dispatchAsync(UUID playerUuid, RWorldRef world, RBlockPos pos, RKey entityTypeKey, boolean cancelled) {
        EntityEventData data = new EntityEventData(playerUuid, world, pos, entityTypeKey, cancelled);
        ListenerList list = asyncListeners.get(EntityEventData.class);
        if (list == null) return;
        Consumer<?>[] listeners = list.snapshot();

        scheduler.runAsync(() -> {
            for (Consumer<?> listener : listeners) {
                dispatchUnchecked(listener, data);
            }
        });
    }

    /**
     * Checks if there are any registered pre-listeners for the given event type.
     *
     * @param type the event class
     * @return true if listeners are registered
     */
    public boolean hasPreListeners(@NotNull Class<? extends GamePreEvent> type) {
        ListenerList list = preListeners.get(type);
        return list != null && list.hasListeners();
    }

    /**
     * Checks if there are any registered post-listeners for the given event type.
     *
     * @param type the event class
     * @return true if listeners are registered
     */
    public boolean hasPostListeners(@NotNull Class<? extends GamePostEvent> type) {
        ListenerList list = postListeners.get(type);
        return list != null && list.hasListeners();
    }

    /**
     * Checks if there are any registered async listeners for the given event type.
     *
     * @param type the event class
     * @return true if listeners are registered
     */
    public boolean hasAsyncListeners(@NotNull Class<? extends GameEventSnapshot> type) {
        ListenerList list = asyncListeners.get(type);
        return list != null && list.hasListeners();
    }

    /**
     * Checks if there are any async listeners registered for entity events.
     *
     * @return true if entity event listeners are registered
     */
    public boolean hasAsyncEntityListeners() {
        ListenerList list = asyncListeners.get(EntityEventData.class);
        return list != null && list.hasListeners();
    }

    /**
     * Registers a listener in the given listener map.
     *
     * @param <E>      the event type
     * @param map      the listener map
     * @param type     the event class
     * @param listener the listener consumer
     * @return a subscription that removes the listener when closed
     */
    private static <E> Subscription register(Map<Class<?>, ListenerList> map, Class<E> type, Consumer<E> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");

        ListenerList list = map.computeIfAbsent(type, k -> new ListenerList());
        list.add(listener);
        return () -> {
            ListenerList current = map.get(type);
            if (current == null) return;

            current.remove(listener);
            if (!current.hasListeners()) {
                map.remove(type, current);
            }
        };
    }

    /**
     * Dispatches an event to a single listener, catching and logging exceptions.
     *
     * @param <E>      the event type
     * @param listener the listener consumer
     * @param event    the event to dispatch
     */
    @SuppressWarnings("unchecked")
    private <E> void dispatchUnchecked(Consumer<?> listener, E event) {
        try {
            ((Consumer<E>) listener).accept(event);
        } catch (Exception e) {
            logger.warn("Unhandled exception in event listener for {}", event.getClass().getName(), e);
        }
    }

    /**
     * Closes the event bus and clears all registered listeners.
     */
    @Override
    public void close() {
        preListeners.clear();
        postListeners.clear();
        asyncListeners.clear();
    }

    /**
     * Simple data holder for entity events (attack, interact, etc.)
     *
     * @param playerUuid    the UUID of the player involved
     * @param world         the world reference
     * @param pos           the block position
     * @param entityTypeKey the entity type key
     * @param cancelled     whether the event was cancelled
     */
    public record EntityEventData(UUID playerUuid, RWorldRef world, RBlockPos pos, RKey entityTypeKey,
                                  boolean cancelled) implements GameEventSnapshot {
    }

    /**
     * Thread-safe list of listeners backed by a volatile array for lock-free reads.
     */
    private static final class ListenerList {
        private static final Consumer<?>[] EMPTY = new Consumer<?>[0];
        private volatile Consumer<?>[] listeners = EMPTY;

        /**
         * Returns a snapshot of the current listeners array.
         *
         * @return the listeners array
         */
        Consumer<?>[] snapshot() {
            return listeners;
        }

        /**
         * Returns whether any listeners are registered.
         *
         * @return true if listeners exist
         */
        boolean hasListeners() {
            return listeners.length != 0;
        }

        /**
         * Adds a listener to the list.
         *
         * @param listener the listener to add
         */
        void add(Consumer<?> listener) {
            Objects.requireNonNull(listener, "listener");
            synchronized (this) {
                Consumer<?>[] current = listeners;
                Consumer<?>[] next = Arrays.copyOf(current, current.length + 1);
                next[current.length] = listener;
                listeners = next;
            }
        }

        /**
         * Removes a listener from the list by identity.
         *
         * @param listener the listener to remove
         */
        void remove(Consumer<?> listener) {
            Objects.requireNonNull(listener, "listener");
            synchronized (this) {
                Consumer<?>[] current = listeners;
                if (current.length == 0) return;

                int index = -1;
                for (int i = 0; i < current.length; i++) {
                    if (current[i] == listener) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) return;

                if (current.length == 1) {
                    listeners = EMPTY;
                    return;
                }

                Consumer<?>[] next = new Consumer<?>[current.length - 1];
                System.arraycopy(current, 0, next, 0, index);
                System.arraycopy(current, index + 1, next, index, current.length - index - 1);
                listeners = next;
            }
        }
    }
}
