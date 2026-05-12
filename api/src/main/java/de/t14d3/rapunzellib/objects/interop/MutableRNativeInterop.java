package de.t14d3.rapunzellib.objects.interop;

import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

/**
 * An {@link RNativeInterop} that supports registering view adapters at runtime.
 */
public interface MutableRNativeInterop extends RNativeInterop {
    /**
     * Registers a view adapter for the given native and view types.
     *
     * @param nativeType the native wrapper type class
     * @param viewType   the target view type class
     * @param adapter    the adapter to register
     * @param <N>        the native wrapper type
     * @param <T>        the target view type
     */
    <N extends RNative, T> void registerViewAdapter(
        @NotNull Class<N> nativeType,
        @NotNull Class<T> viewType,
        @NotNull RNativeViewAdapter<? super N, T> adapter
    );
}
