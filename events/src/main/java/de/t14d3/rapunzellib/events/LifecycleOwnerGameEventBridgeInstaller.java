package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Abstract bridge installer that resolves a lifecycle owner class by name
 * and passes the owner instance to the platform-specific installation method.
 *
 * <p>This is useful for platforms where event registration requires a plugin
 * or mod instance as the owner for proper lifecycle management.</p>
 */
public abstract class LifecycleOwnerGameEventBridgeInstaller extends AbstractGameEventBridgeInstaller {
    private final String ownerTypeName;

    /**
     * Constructs a new lifecycle owner bridge installer.
     *
     * @param platformId      the platform identifier
     * @param supportManifest the support manifest
     * @param ownerTypeName   the fully qualified class name of the lifecycle owner
     */
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

    /**
     * Installs the bridge with the resolved lifecycle owner.
     *
     * @param context the Rapunzel context
     * @param bus     the game event bus
     * @param owner   the resolved lifecycle owner instance
     * @return the installed bridge
     */
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
