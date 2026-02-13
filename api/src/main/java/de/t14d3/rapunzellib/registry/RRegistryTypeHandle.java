package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class RRegistryTypeHandle<H> extends RNativeHandle<H> implements RRegistryType {
    private final RKey key;

    protected RRegistryTypeHandle(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull H handle) {
        super(platformId, handle);
        this.key = Objects.requireNonNull(key, "key");
    }

    protected RRegistryTypeHandle(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull H handle, @NotNull RAttachmentContainer attachments) {
        super(platformId, handle, attachments);
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public final @NotNull RKey key() {
        return key;
    }
}
