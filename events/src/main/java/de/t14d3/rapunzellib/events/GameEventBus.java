package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GameEventBus implements AutoCloseable {
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final Scheduler scheduler;
    private final Logger logger;

    private final Map<Class<?>, ListenerList> preListeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, ListenerList> postListeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, ListenerList> asyncListeners = new ConcurrentHashMap<>();

    public GameEventBus(Scheduler scheduler, Logger logger) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public <E extends CancellablePreEvent> Subscription onPre(Class<E> eventType, Consumer<E> listener) {
        return register(preListeners, eventType, listener);
    }

    public <E extends GamePostEvent> Subscription onPost(Class<E> eventType, Consumer<E> listener) {
        return register(postListeners, eventType, listener);
    }

    public <E extends GameEventSnapshot> Subscription onAsync(Class<E> eventType, Consumer<E> listener) {
        return register(asyncListeners, eventType, listener);
    }

    public <E extends CancellablePreEvent> void dispatchPre(E event) {
        ListenerList list = preListeners.get(event.getClass());
        if (list == null) return;
        Consumer<?>[] listeners = list.snapshot();
        if (listeners.length == 0) return;

        for (int i = 0; i < listeners.length; i++) {
            if (event.isDenied()) return;
            dispatchUnchecked(listeners[i], event);
        }
    }

    public <E extends GamePostEvent> void dispatchPost(E event) {
        ListenerList list = postListeners.get(event.getClass());
        if (list == null) return;
        Consumer<?>[] listeners = list.snapshot();
        if (listeners.length == 0) return;

        for (int i = 0; i < listeners.length; i++) {
            dispatchUnchecked(listeners[i], event);
        }
    }

    public <E extends GameEventSnapshot> void dispatchAsync(E snapshot) {
        ListenerList list = asyncListeners.get(snapshot.getClass());
        if (list == null) return;
        Consumer<?>[] listeners = list.snapshot();
        if (listeners.length == 0) return;

        scheduler.runAsync(() -> {
            for (int i = 0; i < listeners.length; i++) {
                dispatchUnchecked(listeners[i], snapshot);
            }
        });
    }

    public boolean hasPreListeners(Class<? extends GamePreEvent> type) {
        ListenerList list = preListeners.get(type);
        return list != null && list.hasListeners();
    }

    public boolean hasPostListeners(Class<? extends GamePostEvent> type) {
        ListenerList list = postListeners.get(type);
        return list != null && list.hasListeners();
    }

    public boolean hasAsyncListeners(Class<? extends GameEventSnapshot> type) {
        ListenerList list = asyncListeners.get(type);
        return list != null && list.hasListeners();
    }

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

    @SuppressWarnings("unchecked")
    private <E> void dispatchUnchecked(Consumer<?> listener, E event) {
        try {
            ((Consumer<E>) listener).accept(event);
        } catch (Exception e) {
            logger.warn("Unhandled exception in event listener for {}: {}", event.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public void close() {
        preListeners.clear();
        postListeners.clear();
        asyncListeners.clear();
    }

    private static final class ListenerList {
        private static final Consumer<?>[] EMPTY = new Consumer<?>[0];
        private volatile Consumer<?>[] listeners = EMPTY;

        Consumer<?>[] snapshot() {
            return listeners;
        }

        boolean hasListeners() {
            return listeners.length != 0;
        }

        void add(Consumer<?> listener) {
            Objects.requireNonNull(listener, "listener");
            synchronized (this) {
                Consumer<?>[] current = listeners;
                Consumer<?>[] next = Arrays.copyOf(current, current.length + 1);
                next[current.length] = listener;
                listeners = next;
            }
        }

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
