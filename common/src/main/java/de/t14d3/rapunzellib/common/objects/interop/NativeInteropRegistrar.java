package de.t14d3.rapunzellib.common.objects.interop;

import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Fluent builder for registering native view adapters on a {@link MutableRNativeInterop}.
 * <p>
 * Provides a concise API for registering both required and optional view
 * transformations between native wrapper types and their projected view types.
 */
public final class NativeInteropRegistrar {
    /** The mutable interop to register adapters on */
    private final MutableRNativeInterop interop;

    private NativeInteropRegistrar(@NotNull MutableRNativeInterop interop) {
        this.interop = Objects.requireNonNull(interop, "interop");
    }

    public static @NotNull NativeInteropRegistrar create(@NotNull MutableRNativeInterop interop) {
        return new NativeInteropRegistrar(interop);
    }

    public <N extends RNative, T> @NotNull NativeInteropRegistrar view(
        @NotNull Class<N> nativeType,
        @NotNull Class<T> viewType,
        @NotNull Function<? super N, ? extends T> adapter
    ) {
        Objects.requireNonNull(nativeType, "nativeType");
        Objects.requireNonNull(viewType, "viewType");
        Objects.requireNonNull(adapter, "adapter");

        interop.registerViewAdapter(nativeType, viewType, wrapper -> Optional.ofNullable(adapter.apply(wrapper)));
        return this;
    }

    public <N extends RNative, T> @NotNull NativeInteropRegistrar optionalView(
        @NotNull Class<N> nativeType,
        @NotNull Class<T> viewType,
        @NotNull Function<? super N, ? extends Optional<? extends T>> adapter
    ) {
        Objects.requireNonNull(nativeType, "nativeType");
        Objects.requireNonNull(viewType, "viewType");
        Objects.requireNonNull(adapter, "adapter");

        interop.registerViewAdapter(nativeType, viewType, wrapper -> adapter.apply(wrapper).map(viewType::cast));
        return this;
    }
}
