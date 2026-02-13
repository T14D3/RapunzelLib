package de.t14d3.rapunzellib.objects.interop;

import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@FunctionalInterface
public interface RNativeViewAdapter<N extends RNative, T> {
    @NotNull Optional<T> findView(@NotNull N nativeWrapper);
}
