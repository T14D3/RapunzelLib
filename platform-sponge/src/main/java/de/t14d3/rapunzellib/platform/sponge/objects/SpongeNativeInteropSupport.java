package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.common.objects.interop.NativeInteropRegistrar;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;

public final class SpongeNativeInteropSupport {
    private SpongeNativeInteropSupport() {
    }

    public static void register(@NotNull MutableRNativeInterop interop) {
        NativeInteropRegistrar.create(interop)
            .view(SpongePlayer.class, Audience.class, SpongePlayer::audience)
            .view(SpongePlayer.class, ServerPlayer.class, SpongePlayer::handle)
            .view(SpongePlayer.class, Living.class, SpongePlayer::handle)
            .view(SpongePlayer.class, Entity.class, SpongePlayer::handle)
            .view(SpongeEntity.class, Entity.class, SpongeEntity::handle)
            .view(SpongeLivingEntity.class, Living.class, wrapper -> wrapper.handle(Living.class))
            .view(SpongeWorld.class, ServerWorld.class, SpongeWorld::handle)
            .view(SpongeBlock.class, BlockState.class, SpongeBlock::handle)
            .view(SpongeBlockData.class, BlockState.class, SpongeBlockData::handle);
    }
}
