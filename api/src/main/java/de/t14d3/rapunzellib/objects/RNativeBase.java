package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;

import java.util.Objects;

public abstract class RNativeBase implements RNative {
    private final PlatformId platformId;
    private final RExtras extras;

    protected RNativeBase(PlatformId platformId) {
        this(platformId, RExtras.lazyMutable());
    }

    protected RNativeBase(PlatformId platformId, RExtras extras) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.extras = Objects.requireNonNull(extras, "extras");
    }

    @Override
    public final PlatformId platformId() {
        return platformId;
    }

    @Override
    public final RExtras extras() {
        return extras;
    }
}
