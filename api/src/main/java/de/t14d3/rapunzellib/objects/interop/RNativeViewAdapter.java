package de.t14d3.rapunzellib.objects.interop;

import de.t14d3.rapunzellib.objects.RNative;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A functional interface for adapting a native wrapper to a different type view.
 *
 * @param <N> the native wrapper type
 * @param <T> the target view type
 */
@FunctionalInterface
public interface RNativeViewAdapter<N extends RNative, T> {
    /**
     * Attempts to find a view of the target type for the given native wrapper.
     *
     * @param nativeWrapper the native wrapper to adapt
     * @return an {@link Optional} containing the view, or empty if the adaptation is not applicable
     */
    @NotNull Optional<T> findView(@NotNull N nativeWrapper);
}
