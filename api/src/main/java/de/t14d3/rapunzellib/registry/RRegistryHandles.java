package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Utility for resolving native handles from registry type values.
 *
 * <p>Allows obtaining the underlying platform handle from a registry type
 * entry identified by a {@link RRegistryRef}.</p>
 */
public final class RRegistryHandles {
    private RRegistryHandles() {
    }

    /**
     * Finds the native handle of the given type for the registry entry identified by the ref.
     *
     * @param ref        the registry reference
     * @param handleType the expected handle type class
     * @param <T>        the registry type
     * @param <H>        the handle type
     * @return an {@link Optional} containing the handle, or empty if not found
     */
    public static <T extends RRegistryType, H> @NotNull Optional<H> find(
        @NotNull RRegistryRef<T> ref,
        @NotNull Class<H> handleType
    ) {
        Objects.requireNonNull(ref, "ref");
        Class<H> requestedHandleType = Objects.requireNonNull(handleType, "handleType");
        return Rapunzel.findContext()
            .flatMap(context -> context.registries().find(ref))
            .flatMap(value -> value.tryHandle(requestedHandleType));
    }

    /**
     * Requires the native handle of the given type for the registry entry identified by the ref.
     *
     * @param ref        the registry reference
     * @param handleType the expected handle type class
     * @param <T>        the registry type
     * @param <H>        the handle type
     * @return the handle
     * @throws IllegalArgumentException if the handle is not found or of the wrong type
     */
    public static <T extends RRegistryType, H> @NotNull H require(
        @NotNull RRegistryRef<T> ref,
        @NotNull Class<H> handleType
    ) {
        Objects.requireNonNull(ref, "ref");
        Class<H> requestedHandleType = Objects.requireNonNull(handleType, "handleType");
        return find(ref, requestedHandleType).orElseThrow(() -> new IllegalArgumentException(
            "Registry entry " + ref.key() + " does not expose handle type " + requestedHandleType.getName()
        ));
    }
}
