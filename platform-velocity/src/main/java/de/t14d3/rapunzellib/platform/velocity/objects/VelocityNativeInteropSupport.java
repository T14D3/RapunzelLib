package de.t14d3.rapunzellib.platform.velocity.objects;

import com.velocitypowered.api.proxy.Player;
import de.t14d3.rapunzellib.common.objects.interop.NativeInteropRegistrar;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

public final class VelocityNativeInteropSupport {
    private VelocityNativeInteropSupport() {
    }

    public static void register(@NotNull MutableRNativeInterop interop) {
        NativeInteropRegistrar.create(interop)
            .view(VelocityPlayer.class, Audience.class, VelocityPlayer::audience)
            .view(VelocityPlayer.class, Player.class, VelocityPlayer::handle);
    }
}
