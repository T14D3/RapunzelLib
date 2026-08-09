package de.t14d3.rapunzellib.events.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.LifecycleOwnerGameEventBridgeInstaller;
import org.jetbrains.annotations.NotNull;

/**
 * Installs the Velocity {@link VelocityGameEventsBridge} when the proxy
 * platform is bootstrapped.
 *
 * <p>The lifecycle owner is the RapunzelLib velocity platform plugin
 * ({@code de.t14d3.rapunzellib.platform.velocity.VelocityPlatformPlugin} -
 * resolved by name to avoid a compile dependency on the platform module).
 * The velocity {@code Plugin} API type cannot serve as the owner type: it is
 * an annotation, not a marker interface. The {@link ProxyServer} is resolved
 * from the registered context service.</p>
 */
public final class VelocityGameEventBridgeInstaller extends LifecycleOwnerGameEventBridgeInstaller {
    public VelocityGameEventBridgeInstaller() {
        super(
            PlatformId.VELOCITY,
            VelocityGameEventSupport.MANIFEST,
            "de.t14d3.rapunzellib.platform.velocity.VelocityPlatformPlugin"
        );
    }

    @Override
    protected @NotNull GameEventBridge installBridge(
        @NotNull RapunzelContext context,
        @NotNull GameEventBus bus,
        @NotNull Object owner
    ) {
        ProxyServer proxy = context.services().get(ProxyServer.class);
        return new VelocityGameEventsBridge(bus, proxy, owner);
    }
}
