package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;

import java.util.Objects;

public abstract class RNativeHandle<H> extends RNativeBase {
    private volatile H handle;

    protected RNativeHandle(PlatformId platformId, H handle) {
        super(platformId);
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    protected RNativeHandle(PlatformId platformId, H handle, RExtras extras) {
        super(platformId, extras);
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    @Override
    public H handle() {
        return handle;
    }

    protected final void updateNativeHandle(H newHandle) {
        this.handle = Objects.requireNonNull(newHandle, "newHandle");
    }
}
