package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DefaultItemStackAdapters implements ItemStackAdapters {
    private final PlatformId platformId;
    private final Map<Class<?>, ItemStackAdapter<?>> adapters = new LinkedHashMap<>();

    public DefaultItemStackAdapters(@NotNull PlatformId platformId) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    @Override
    public @NotNull PlatformId platformId() {
        return platformId;
    }

    public <T> @NotNull DefaultItemStackAdapters register(
        @NotNull Class<T> handleType,
        @NotNull ItemStackAdapter<? extends T> adapter
    ) {
        Objects.requireNonNull(handleType, "handleType");
        Objects.requireNonNull(adapter, "adapter");
        synchronized (adapters) {
            adapters.put(handleType, adapter);
        }
        return this;
    }

    @Override
    public <T> @NotNull Optional<ItemStackAdapter<T>> find(@NotNull Class<T> handleType) {
        Objects.requireNonNull(handleType, "handleType");
        synchronized (adapters) {
            ItemStackAdapter<?> exact = adapters.get(handleType);
            if (exact != null) {
                return Optional.of(cast(exact));
            }
            for (Map.Entry<Class<?>, ItemStackAdapter<?>> entry : adapters.entrySet()) {
                if (entry.getKey().isAssignableFrom(handleType)) {
                    return Optional.of(cast(entry.getValue()));
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public <T> @NotNull ItemStackAdapter<T> require(@NotNull Class<T> handleType) {
        return find(handleType).orElseThrow(() -> new IllegalStateException(missingHandleTypeMessage(handleType)));
    }

    @Override
    public @NotNull Optional<ItemStackAdapter<Object>> find(@NotNull Object handle) {
        Objects.requireNonNull(handle, "handle");
        synchronized (adapters) {
            @SuppressWarnings("unchecked")
            Class<Object> handleType = (Class<Object>) handle.getClass();
            Optional<ItemStackAdapter<Object>> exact = find(handleType);
            if (exact.isPresent() && exact.orElseThrow().supports(handle)) {
                return exact;
            }
            for (Map.Entry<Class<?>, ItemStackAdapter<?>> entry : adapters.entrySet()) {
                if (entry.getKey().isInstance(handle) && entry.getValue().supports(handle)) {
                    return Optional.of(cast(entry.getValue()));
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public @NotNull ItemStackAdapter<Object> require(@NotNull Object handle) {
        Objects.requireNonNull(handle, "handle");
        return find(handle).orElseThrow(() -> new IllegalStateException(
            "No ItemStackAdapter registered for handle type " + handle.getClass().getName() +
                " on platform " + platformId + ". Registered handle types: " + describeHandleTypes() + '.'
        ));
    }

    @Override
    public @NotNull List<Class<?>> handleTypes() {
        synchronized (adapters) {
            return List.copyOf(adapters.keySet());
        }
    }

    private @NotNull String missingHandleTypeMessage(@NotNull Class<?> handleType) {
        return "No ItemStackAdapter registered for handle type " + handleType.getName() +
            " on platform " + platformId + ". Registered handle types: " + describeHandleTypes() + '.';
    }

    private @NotNull String describeHandleTypes() {
        synchronized (adapters) {
            if (adapters.isEmpty()) {
                return "<none>";
            }
            return adapters.keySet().stream().map(Class::getName).collect(Collectors.joining(", "));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull ItemStackAdapter<T> cast(@NotNull ItemStackAdapter<?> adapter) {
        return (ItemStackAdapter<T>) adapter;
    }
}
