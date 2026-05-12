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

/**
 * Default implementation of {@link MutableRNativeInterop} that stores view adapters
 * in a concurrent map and resolves them via hierarchical type lookup.
 * <p>
 * Adapter resolution walks the superclass and interface hierarchy of the native
 * wrapper type to find a matching adapter for the requested view type.
 */
public final class DefaultRNativeInterop implements MutableRNativeInterop {
    /** The platform this interop belongs to */
    private final PlatformId platformId;
    /** Map of native type -> (view type -> adapter) */
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, RNativeViewAdapter<?, ?>>> adapters =
        new ConcurrentHashMap<>();

    /**
     * Creates a new interop for the given platform.
     *
     * @param platformId the platform identifier
     */
    public DefaultRNativeInterop(@NotNull PlatformId platformId) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    /**
     * Registers a view adapter that can project a native wrapper into a view type.
     *
     * @param nativeType the native wrapper class
     * @param viewType   the projected view type
     * @param adapter    the adapter that performs the projection
     * @param <N>        the native wrapper type
     * @param <T>        the view type
     */
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

    /**
     * Attempts to find a projected view of the given native wrapper.
     *
     * @param nativeWrapper the native wrapper instance
     * @param type          the desired view type
     * @param <T>           the view type
     * @return an optional containing the view, or empty if not available
     */
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

    /**
     * Walks the type hierarchy to find a matching adapter.
     *
     * @param nativeType the native wrapper class
     * @param viewType   the requested view type
     * @param <T>        the view type
     * @return the matching adapter, or null if none found
     */
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
