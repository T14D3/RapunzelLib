package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.attachments.RAttachmentHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Base interface for all native-platform object wrappers in RapunzelLib.
 *
 * <p>Provides access to the underlying platform handle, platform identification,
 * and attachment support.</p>
 */
public interface RNative extends RAttachmentHolder {
    /**
     * Returns the platform identifier for this wrapper.
     *
     * @return the platform ID
     */
    @NotNull PlatformId platformId();

    /**
     * Returns the raw native platform handle.
     *
     * @return the native handle
     */
    @NotNull Object handle();

    /**
     * Returns the native handle cast to the given type, or throws.
     *
     * @param type the expected handle type
     * @param <T>  the handle type
     * @return the typed native handle
     * @throws ClassCastException if the handle cannot be cast or resolved via interop
     */
    default <T> @NotNull T handle(@NotNull Class<T> type) {
        Object handle = handle();
        if (type.isInstance(handle)) return type.cast(handle);
        return tryInteropHandle(type).orElseThrow(() -> new ClassCastException(
            "Cannot resolve native handle of type " + type.getName() + " from " + handle.getClass().getName()
        ));
    }

    /**
     * Attempts to return the native handle cast to the given type.
     *
     * @param type the expected handle type
     * @param <T>  the handle type
     * @return an {@link Optional} containing the typed handle, or empty if not resolvable
     */
    default <T> @NotNull Optional<T> tryHandle(@NotNull Class<T> type) {
        Object handle = handle();
        if (type.isInstance(handle)) return Optional.of(type.cast(handle));
        return tryInteropHandle(type);
    }

    /**
     * A small, per-wrapper key/value store intended for per-project extensions.
     * <p>
     * Platform implementations should return a mutable implementation so plugins can attach
     * additional data to wrapper instances.
     */
    @Override
    default @NotNull RAttachmentContainer attachments() {
        return RAttachmentContainer.empty();
    }

    private <T> @NotNull Optional<T> tryInteropHandle(@NotNull Class<T> type) {
        return Rapunzel.nativeInterop().flatMap(interop -> interop.findView(this, type));
    }
}
