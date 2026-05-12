package de.t14d3.rapunzellib.objects.interop;

import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Service interface for finding view adapters that expose a native wrapper through
 * alternative type interfaces.
 */
public interface RNativeInterop {
    /**
     * Attempts to find a view of the given type for the specified native wrapper.
     *
     * @param nativeWrapper the native wrapper to adapt
     * @param type          the desired view type class
     * @param <T>           the view type
     * @return an {@link Optional} containing the view, or empty if no adapter is registered
     */
    <T> @NotNull Optional<T> findView(@NotNull RNative nativeWrapper, @NotNull Class<T> type);
}
