package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.common.objects.interop.NativeInteropRegistrar;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.platform.paper.PaperHandleBridge;
import net.kyori.adventure.audience.Audience;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PaperNativeInteropSupport {
    private PaperNativeInteropSupport() {
    }

    public static void register(@NotNull MutableRNativeInterop interop) {
        NativeInteropRegistrar.create(interop)
            .view(PaperPlayer.class, Player.class, wrapper -> PaperHandleBridge.toBukkit(wrapper.handle()))
            .view(PaperPlayer.class, Audience.class, wrapper -> PaperHandleBridge.toBukkit(wrapper.handle()))
            .view(PaperEntity.class, Entity.class, wrapper -> PaperHandleBridge.toBukkit(wrapper.handle()))
            .view(
                PaperLivingEntity.class,
                LivingEntity.class,
                wrapper -> (LivingEntity) PaperHandleBridge.toBukkit(wrapper.handle(net.minecraft.world.entity.LivingEntity.class))
            )
            .optionalView(PaperWorld.class, World.class, wrapper -> PaperHandleBridge.toBukkit(wrapper.handle()))
            .view(PaperBlockData.class, BlockData.class, wrapper -> PaperHandleBridge.toBukkit(wrapper.handle()));
    }
}
