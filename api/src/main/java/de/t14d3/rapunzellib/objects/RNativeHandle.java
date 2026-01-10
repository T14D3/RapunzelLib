package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class RNativeHandle<H> extends RNativeBase {
    private volatile H handle;

    protected RNativeHandle(@NotNull PlatformId platformId, @NotNull H handle) {
        super(platformId);
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    protected RNativeHandle(@NotNull PlatformId platformId, @NotNull H handle, @NotNull RExtras extras) {
        super(platformId, extras);
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    @Override
    public @NotNull H handle() {
        return handle;
    }

    public final void updateNativeHandle(@NotNull H newHandle) {
        this.handle = Objects.requireNonNull(newHandle, "newHandle");
    }
}
