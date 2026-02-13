package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.platform.shared.entity.SharedNativeInteropSupport;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FabricNativeInteropSupport {
    private FabricNativeInteropSupport() {
    }

    public static void register(@NotNull MutableRNativeInterop interop) {
        Objects.requireNonNull(interop, "interop");
        SharedNativeInteropSupport.register(
            interop,
            FabricPlayer.class,
            FabricPlayer::audience,
            FabricEntity.class,
            FabricLivingEntity.class,
            FabricWorld.class,
            FabricBlock.class,
            FabricBlockData.class
        );
    }
}
