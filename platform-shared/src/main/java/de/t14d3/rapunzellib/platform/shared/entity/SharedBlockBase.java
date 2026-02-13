package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.InventoryInteropSupport;
import de.t14d3.rapunzellib.objects.RContainer;
import de.t14d3.rapunzellib.objects.RNativeBase;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public abstract class SharedBlockBase extends RNativeBase implements RBlock {
    private final ServerLevel world;
    private final BlockPos pos;
    private final SharedWorldHooks worldHooks;
    private final Function<BlockState, ? extends RBlockData> blockDataFactory;

    protected SharedBlockBase(
        @NotNull PlatformId platformId,
        @NotNull ServerLevel world,
        @NotNull BlockPos pos,
        @NotNull Function<ServerLevel, ? extends RWorld> worldFactory,
        @NotNull Function<BlockState, ? extends RBlockData> blockDataFactory
    ) {
        this(platformId, world, pos, RAttachmentContainer.lazyMutable(), SharedWorldHooks.of(worldFactory), blockDataFactory);
    }

    protected SharedBlockBase(
        @NotNull PlatformId platformId,
        @NotNull ServerLevel world,
        @NotNull BlockPos pos,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks,
        @NotNull Function<BlockState, ? extends RBlockData> blockDataFactory
    ) {
        super(platformId, Objects.requireNonNull(attachments, "attachments"));
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.worldHooks = Objects.requireNonNull(worldHooks, "worldHooks");
        this.blockDataFactory = Objects.requireNonNull(blockDataFactory, "blockDataFactory");
    }

    @Override
    public final @NotNull BlockState handle() {
        return world.getBlockState(pos);
    }

    @Override
    public final @NotNull RWorld world() {
        return worldHooks.createWorld(world);
    }

    @Override
    public final @NotNull RBlockPos pos() {
        return new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public final @NotNull RRegistryRef<RBlockType> typeRef() {
        return RBlockType.ref(BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).toString());
    }

    @Override
    public final @NotNull RBlockData data() {
        return blockDataFactory.apply(world.getBlockState(pos));
    }

    @Override
    public final boolean canSetData() {
        return true;
    }

    @Override
    public final boolean setData(@NotNull RBlockData data) {
        Objects.requireNonNull(data, "data");
        return world.setBlock(pos, data.handle(BlockState.class), Block.UPDATE_ALL);
    }

    @Override
    public final @NotNull Optional<RContainer> container() {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            return Optional.empty();
        }
        return InventoryInteropSupport.wrapInventory(blockEntity);
    }
}
