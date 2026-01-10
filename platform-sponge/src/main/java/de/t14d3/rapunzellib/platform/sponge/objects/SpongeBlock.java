package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RNativeBase;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;

final class SpongeBlock extends RNativeBase implements RBlock {
    private final ServerWorld world;
    private final Vector3i pos;

    SpongeBlock(ServerWorld world, Vector3i pos) {
        super(PlatformId.SPONGE);
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
    }

    @Override
    public @NotNull BlockState handle() {
        return world.block(pos);
    }

    @Override
    public @NotNull RWorld world() {
        return Rapunzel.context().worlds()
            .wrap(world)
            .orElseGet(() -> new SpongeWorld(world));
    }

    @Override
    public @NotNull RBlockPos pos() {
        return new RBlockPos(pos.x(), pos.y(), pos.z());
    }

    @Override
    public @NotNull String typeKey() {
        BlockState state = world.block(pos);
        return state.type().key(RegistryTypes.BLOCK_TYPE).asString();
    }

    @Override
    public @NotNull RBlockData data() {
        return new SpongeBlockData(world.block(pos));
    }
}

