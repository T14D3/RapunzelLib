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

/**
 * Abstract base implementation of {@link RBlock}, wrapping a block at a specific world position.
 * <p>
 * Provides type reference, block data, and container (inventory) access, delegating
 * world creation to {@link SharedWorldHooks}. Block state is fetched live from the world.
 * </p>
 */
public abstract class SharedBlockBase extends RNativeBase implements RBlock {
    private final ServerLevel world;
    private final BlockPos pos;
    private final SharedWorldHooks worldHooks;
    private final Function<BlockState, ? extends RBlockData> blockDataFactory;

    /**
     * Constructs a new block base with a lazy-mutable attachment container, a world factory, and a block data factory.
     *
     * @param platformId        the platform identifier
     * @param world             the server level containing this block
     * @param pos               the block position
     * @param worldFactory      a function to create {@link RWorld} wrappers from {@link ServerLevel} instances
     * @param blockDataFactory  a function to create {@link RBlockData} wrappers from {@link BlockState} instances
     */
    protected SharedBlockBase(
        @NotNull PlatformId platformId,
        @NotNull ServerLevel world,
        @NotNull BlockPos pos,
        @NotNull Function<ServerLevel, ? extends RWorld> worldFactory,
        @NotNull Function<BlockState, ? extends RBlockData> blockDataFactory
    ) {
        this(platformId, world, pos, RAttachmentContainer.lazyMutable(), SharedWorldHooks.of(worldFactory), blockDataFactory);
    }

    /**
     * Constructs a new block base with explicit attachments, world hooks, and block data factory.
     *
     * @param platformId        the platform identifier
     * @param world             the server level containing this block
     * @param pos               the block position
     * @param attachments       the attachment container for this block
     * @param worldHooks        shared world creation hooks
     * @param blockDataFactory  a function to create {@link RBlockData} wrappers from {@link BlockState} instances
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull BlockState handle() {
        return world.getBlockState(pos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull RWorld world() {
        return worldHooks.createWorld(world);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull RBlockPos pos() {
        return new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull RRegistryRef<RBlockType> typeRef() {
        return RBlockType.ref(BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull RBlockData data() {
        return blockDataFactory.apply(world.getBlockState(pos));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canSetData() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean setData(@NotNull RBlockData data) {
        Objects.requireNonNull(data, "data");
        return world.setBlock(pos, data.handle(BlockState.class), Block.UPDATE_ALL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<RContainer> container() {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            return Optional.empty();
        }
        return InventoryInteropSupport.wrapInventory(blockEntity);
    }
}
