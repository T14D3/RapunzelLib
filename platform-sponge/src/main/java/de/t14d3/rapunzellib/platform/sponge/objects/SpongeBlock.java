package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.InventoryInteropSupport;
import de.t14d3.rapunzellib.objects.RContainer;
import de.t14d3.rapunzellib.objects.RNativeBase;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;
import java.util.Optional;

final class SpongeBlock extends RNativeBase implements RBlock {
    private final ServerWorld world;
    private final Vector3i pos;
    private final SpongeWorlds worlds;

    SpongeBlock(ServerWorld world, Vector3i pos, @NotNull SpongeAttachmentService attachmentService, @NotNull SpongeWorlds worlds) {
        super(PlatformId.SPONGE, Objects.requireNonNull(attachmentService, "attachmentService").forBlock(world, pos));
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
    }

    @Override
    public @NotNull BlockState handle() {
        return world.block(pos);
    }

    @Override
    public @NotNull RWorld world() {
        return worlds.requireNative(world);
    }

    @Override
    public @NotNull RBlockPos pos() {
        return new RBlockPos(pos.x(), pos.y(), pos.z());
    }

    @Override
    public @NotNull RRegistryRef<RBlockType> typeRef() {
        return RBlockType.ref(world.block(pos).type().key(RegistryTypes.BLOCK_TYPE).asString());
    }

    @Override
    public @NotNull RBlockData data() {
        return new SpongeBlockData(world.block(pos));
    }

    @Override
    public boolean canSetData() {
        return true;
    }

    @Override
    public boolean setData(@NotNull RBlockData data) {
        Objects.requireNonNull(data, "data");
        return world.setBlock(pos, data.handle(BlockState.class));
    }

    @Override
    public @NotNull Optional<RContainer> container() {
        return world.blockEntity(pos).flatMap(SpongeBlock::wrapContainerInventory);
    }

    private static @NotNull Optional<RContainer> wrapContainerInventory(@NotNull BlockEntity blockEntity) {
        Objects.requireNonNull(blockEntity, "blockEntity");
        if (!(blockEntity instanceof Carrier carrier)) {
            return Optional.empty();
        }
        return InventoryInteropSupport.wrapInventory(carrier.inventory());
    }
}
