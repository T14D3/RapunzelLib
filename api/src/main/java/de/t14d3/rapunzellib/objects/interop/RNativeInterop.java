package de.t14d3.rapunzellib.objects.interop;

import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RNativeInterop {
    <T> @NotNull Optional<T> findView(@NotNull RNative nativeWrapper, @NotNull Class<T> type);
}
