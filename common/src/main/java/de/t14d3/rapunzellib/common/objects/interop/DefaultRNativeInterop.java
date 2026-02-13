package de.t14d3.rapunzellib.common.objects.interop;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.objects.interop.RNativeViewAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultRNativeInterop implements MutableRNativeInterop {
    private final PlatformId platformId;
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, RNativeViewAdapter<?, ?>>> adapters =
        new ConcurrentHashMap<>();

    public DefaultRNativeInterop(@NotNull PlatformId platformId) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    @Override
    public <N extends RNative, T> void registerViewAdapter(
        @NotNull Class<N> nativeType,
        @NotNull Class<T> viewType,
        @NotNull RNativeViewAdapter<? super N, T> adapter
    ) {
        Objects.requireNonNull(nativeType, "nativeType");
        Objects.requireNonNull(viewType, "viewType");
        Objects.requireNonNull(adapter, "adapter");

        adapters.computeIfAbsent(nativeType, ignored -> new ConcurrentHashMap<>()).put(viewType, adapter);
    }

    @Override
    public <T> @NotNull Optional<T> findView(@NotNull RNative nativeWrapper, @NotNull Class<T> type) {
        Objects.requireNonNull(nativeWrapper, "nativeWrapper");
        Objects.requireNonNull(type, "type");
        if (nativeWrapper.platformId() != platformId) {
            return Optional.empty();
        }

        RNativeViewAdapter<RNative, T> adapter = findAdapter(nativeWrapper.getClass(), type);
        if (adapter == null) {
            return Optional.empty();
        }
        return adapter.findView(nativeWrapper).map(type::cast);
    }

    @SuppressWarnings("unchecked")
    private <T> RNativeViewAdapter<RNative, T> findAdapter(Class<?> nativeType, Class<T> viewType) {
        ArrayDeque<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> seen = new HashSet<>();
        queue.add(nativeType);

        while (!queue.isEmpty()) {
            Class<?> currentType = queue.removeFirst();
            if (!seen.add(currentType)) {
                continue;
            }

            Map<Class<?>, RNativeViewAdapter<?, ?>> adaptersByViewType = adapters.get(currentType);
            if (adaptersByViewType != null) {
                RNativeViewAdapter<?, ?> adapter = adaptersByViewType.get(viewType);
                if (adapter != null) {
                    return (RNativeViewAdapter<RNative, T>) adapter;
                }
            }

            Class<?> superType = currentType.getSuperclass();
            if (superType != null) {
                queue.addLast(superType);
            }
            for (Class<?> interfaceType : currentType.getInterfaces()) {
                queue.addLast(interfaceType);
            }
        }

        return null;
    }
}
