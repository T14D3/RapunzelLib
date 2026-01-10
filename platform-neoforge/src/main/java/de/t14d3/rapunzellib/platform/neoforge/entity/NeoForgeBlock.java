package de.t14d3.rapunzellib.platform.neoforge.entity;

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
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class NeoForgeBlock extends RNativeBase implements RBlock {
    private final ServerLevel world;
    private final BlockPos pos;

    NeoForgeBlock(ServerLevel world, BlockPos pos) {
        super(PlatformId.NEOFORGE);
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
    }

    @Override
    public @NotNull BlockState handle() {
        return world.getBlockState(pos);
    }

    @Override
    public @NotNull RWorld world() {
        return Rapunzel.context().worlds()
            .wrap(world)
            .orElseGet(() -> new NeoForgeWorld(world));
    }

    @Override
    public @NotNull RBlockPos pos() {
        return new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public @NotNull String typeKey() {
        BlockState state = world.getBlockState(pos);
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    @Override
    public @NotNull RBlockData data() {
        return new NeoForgeBlockData(world.getBlockState(pos));
    }
}
