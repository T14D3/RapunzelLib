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

/**
 * Static utility for registering native interop views between RapunzelLib wrapper types
 * and Minecraft native types.
 * <p>
 * Allows wrapper types to expose their native handles as specific Minecraft types
 * (e.g. viewing an {@code RBlock} as a {@link BlockState}).
 * </p>
 */
public final class SharedNativeInteropSupport {
    private SharedNativeInteropSupport() {
    }

    /**
     * Registers native interop views for all standard platform wrapper types.
     * <p>
     * Registers views for players, entities, living entities, worlds, blocks, and block data.
     * Each wrapper type is bound to its corresponding Minecraft native type.
     * </p>
     *
     * @param <P>             the player wrapper type
     * @param <E>             the entity wrapper type
     * @param <L>             the living entity wrapper type
     * @param <W>             the world wrapper type
     * @param <B>             the block wrapper type
     * @param <D>             the block data wrapper type
     * @param interop        the mutable interop registry to configure
     * @param playerType     the player wrapper class
     * @param audienceView   a function to convert a player wrapper to an Adventure {@link Audience}
     * @param entityType     the entity wrapper class
     * @param livingEntityType the living entity wrapper class
     * @param worldType      the world wrapper class
     * @param blockType      the block wrapper class
     * @param blockDataType  the block data wrapper class
     */
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
