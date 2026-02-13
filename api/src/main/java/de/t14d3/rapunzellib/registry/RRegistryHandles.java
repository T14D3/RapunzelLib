package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class RRegistryHandles {
    private RRegistryHandles() {
    }

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
