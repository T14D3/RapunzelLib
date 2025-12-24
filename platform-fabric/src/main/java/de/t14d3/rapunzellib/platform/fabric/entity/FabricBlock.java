package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RNativeBase;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

final class FabricBlock extends RNativeBase implements RBlock {
    private final ServerLevel world;
    private final BlockPos pos;

    FabricBlock(ServerLevel world, BlockPos pos) {
        super(PlatformId.FABRIC);
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
    }

    @Override
    public BlockState handle() {
        return world.getBlockState(pos);
    }

    @Override
    public RWorld world() {
        return Rapunzel.context().worlds()
            .wrap(world)
            .orElseGet(() -> new FabricWorld(world));
    }

    @Override
    public RBlockPos pos() {
        return new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public String typeKey() {
        BlockState state = world.getBlockState(pos);
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    @Override
    public RBlockData data() {
        return new FabricBlockData(world.getBlockState(pos));
    }
}

