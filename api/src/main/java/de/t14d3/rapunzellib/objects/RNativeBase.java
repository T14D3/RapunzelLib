package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class RNativeBase implements RNative {
    private final PlatformId platformId;
    private final RExtras extras;

    protected RNativeBase(@NotNull PlatformId platformId) {
        this(platformId, RExtras.lazyMutable());
    }

    protected RNativeBase(@NotNull PlatformId platformId, @NotNull RExtras extras) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.extras = Objects.requireNonNull(extras, "extras");
    }

    @Override
    public final @NotNull PlatformId platformId() {
        return platformId;
    }

    @Override
    public final @NotNull RExtras extras() {
        return extras;
    }
}
