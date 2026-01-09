package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;
import java.util.Optional;

public final class SpongeBlocks implements Blocks {
    @Override
    public Optional<RBlock> wrap(Object nativeBlock) {
        if (nativeBlock instanceof ServerLocation loc) {
            return Optional.of(new SpongeBlock(loc.world(), loc.blockPosition()));
        }
        if (nativeBlock instanceof LocatableBlock locatable) {
            if (!(locatable.world() instanceof ServerWorld world)) return Optional.empty();
            return Optional.of(new SpongeBlock(world, locatable.blockPosition()));
        }
        if (nativeBlock instanceof BlockSnapshot snapshot) {
            if (!Sponge.isServerAvailable()) return Optional.empty();
            Vector3i pos = snapshot.position();
            return Sponge.server().worldManager().world(snapshot.world())
                .map(world -> new SpongeBlock(world, pos));
        }
        return Optional.empty();
    }

    @Override
    public Optional<RBlockData> wrapData(Object nativeBlockData) {
        if (nativeBlockData instanceof BlockState state) return Optional.of(new SpongeBlockData(state));
        if (nativeBlockData instanceof BlockType type) return Optional.of(new SpongeBlockData(type.defaultState()));
        if (nativeBlockData instanceof BlockSnapshot snapshot) return Optional.of(new SpongeBlockData(snapshot.state()));
        if (nativeBlockData instanceof LocatableBlock locatable) return Optional.of(new SpongeBlockData(locatable.blockState()));
        if (nativeBlockData instanceof ServerLocation loc) return Optional.of(new SpongeBlockData(loc.block()));
        return Optional.empty();
    }

    @Override
    public RBlock at(RWorld world, RBlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        ServerWorld spongeWorld = world.handle(ServerWorld.class);
        return new SpongeBlock(spongeWorld, new Vector3i(pos.x(), pos.y(), pos.z()));
    }

    @Override
    public Optional<RBlockData> parseData(String value) {
        if (value == null) return Optional.empty();
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return Optional.empty();

        try {
            BlockState state = BlockState.fromString(trimmed);
            return Optional.of(new SpongeBlockData(state));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
