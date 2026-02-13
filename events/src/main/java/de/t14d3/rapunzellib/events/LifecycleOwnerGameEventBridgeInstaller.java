package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class LifecycleOwnerGameEventBridgeInstaller extends AbstractGameEventBridgeInstaller {
    private final String ownerTypeName;

    protected LifecycleOwnerGameEventBridgeInstaller(
        @NotNull PlatformId platformId,
        @NotNull GameEventSupportManifest supportManifest,
        @NotNull String ownerTypeName
    ) {
        super(platformId, supportManifest);
        this.ownerTypeName = Objects.requireNonNull(ownerTypeName, "ownerTypeName");
    }

    @Override
    protected final @NotNull GameEventBridge installBridge(@NotNull RapunzelContext context, @NotNull GameEventBus bus) {
        return installBridge(context, bus, context.requireLifecycleOwner(loadOwnerType(ownerTypeName)));
    }

    protected abstract @NotNull GameEventBridge installBridge(
        @NotNull RapunzelContext context,
        @NotNull GameEventBus bus,
        @NotNull Object owner
    );

    private static @NotNull Class<?> loadOwnerType(@NotNull String ownerTypeName) {
        try {
            return Class.forName(ownerTypeName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing lifecycle owner type " + ownerTypeName, e);
        }
    }
}
