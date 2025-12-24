package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

final class PaperBlockData extends RNativeHandle<BlockData> implements RBlockData {
    PaperBlockData(BlockData data) {
        super(PlatformId.PAPER, Objects.requireNonNull(data, "data"));
    }

    @Override
    public String typeKey() {
        return handle().getMaterial().getKey().toString();
    }

    @Override
    public String asString() {
        return handle().getAsString(true);
    }
}

