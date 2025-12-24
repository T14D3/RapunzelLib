package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import org.bukkit.block.Block;

import java.util.Objects;

final class PaperBlock extends RNativeHandle<Block> implements RBlock {
    PaperBlock(Block block) {
        super(PlatformId.PAPER, Objects.requireNonNull(block, "block"));
    }

    @Override
    public RWorld world() {
        Block block = handle();
        return Rapunzel.context().worlds()
            .wrap(block.getWorld())
            .orElseGet(() -> new PaperWorld(block.getWorld()));
    }

    @Override
    public RBlockPos pos() {
        Block block = handle();
        return new RBlockPos(block.getX(), block.getY(), block.getZ());
    }

    @Override
    public String typeKey() {
        return handle().getType().getKey().toString();
    }

    @Override
    public RBlockData data() {
        return new PaperBlockData(handle().getBlockData());
    }
}

