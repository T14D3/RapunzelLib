package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.common.objects.interop.NativeInteropRegistrar;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public final class SharedNativeInteropSupport {
    private SharedNativeInteropSupport() {
    }

    public static <P extends RNative, E extends RNative, L extends RNative, W extends RNative, B extends RNative, D extends RNative> void register(
        @NotNull MutableRNativeInterop interop,
        @NotNull Class<P> playerType,
        @NotNull Function<? super P, ? extends Audience> audienceView,
        @NotNull Class<E> entityType,
        @NotNull Class<L> livingEntityType,
        @NotNull Class<W> worldType,
        @NotNull Class<B> blockType,
        @NotNull Class<D> blockDataType
    ) {
        Objects.requireNonNull(interop, "interop");
        Objects.requireNonNull(playerType, "playerType");
        Objects.requireNonNull(audienceView, "audienceView");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(livingEntityType, "livingEntityType");
        Objects.requireNonNull(worldType, "worldType");
        Objects.requireNonNull(blockType, "blockType");
        Objects.requireNonNull(blockDataType, "blockDataType");

        NativeInteropRegistrar.create(interop)
            .view(playerType, Audience.class, audienceView::apply)
            .view(playerType, ServerPlayer.class, wrapper -> (ServerPlayer) wrapper.handle())
            .view(playerType, LivingEntity.class, wrapper -> (LivingEntity) wrapper.handle())
            .view(playerType, Entity.class, wrapper -> (Entity) wrapper.handle())
            .view(entityType, Entity.class, wrapper -> (Entity) wrapper.handle())
            .view(livingEntityType, LivingEntity.class, wrapper -> (LivingEntity) wrapper.handle())
            .view(worldType, ServerLevel.class, wrapper -> (ServerLevel) wrapper.handle())
            .view(blockType, BlockState.class, wrapper -> (BlockState) wrapper.handle())
            .view(blockDataType, BlockState.class, wrapper -> (BlockState) wrapper.handle());
    }
}
