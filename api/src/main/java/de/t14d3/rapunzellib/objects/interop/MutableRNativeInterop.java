package de.t14d3.rapunzellib.objects.interop;

import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

public interface MutableRNativeInterop extends RNativeInterop {
    <N extends RNative, T> void registerViewAdapter(
        @NotNull Class<N> nativeType,
        @NotNull Class<T> viewType,
        @NotNull RNativeViewAdapter<? super N, T> adapter
    );
}
