package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.platform.shared.entity.SharedNativeInteropSupport;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class NeoForgeNativeInteropSupport {
    private NeoForgeNativeInteropSupport() {
    }

    public static void register(@NotNull MutableRNativeInterop interop) {
        Objects.requireNonNull(interop, "interop");
        SharedNativeInteropSupport.register(
            interop,
            NeoForgePlayer.class,
            NeoForgePlayer::audience,
            NeoForgeEntity.class,
            NeoForgeLivingEntity.class,
            NeoForgeWorld.class,
            NeoForgeBlock.class,
            NeoForgeBlockData.class
        );
    }
}
