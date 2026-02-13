package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.attachments.RAttachmentHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RNative extends RAttachmentHolder {
    @NotNull PlatformId platformId();

    @NotNull Object handle();

    default <T> @NotNull T handle(@NotNull Class<T> type) {
        Object handle = handle();
        if (type.isInstance(handle)) return type.cast(handle);
        return tryInteropHandle(type).orElseThrow(() -> new ClassCastException(
            "Cannot resolve native handle of type " + type.getName() + " from " + handle.getClass().getName()
        ));
    }

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
