package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class PaperBlocks implements Blocks {
    @Override
    public Optional<RBlock> wrap(Object nativeBlock) {
        return (nativeBlock instanceof Block block) ? Optional.of(new PaperBlock(block)) : Optional.empty();
    }

    @Override
    public Optional<RBlockData> wrapData(Object nativeBlockData) {
        if (nativeBlockData instanceof BlockData data) return Optional.of(new PaperBlockData(data));
        if (nativeBlockData instanceof Block block) return Optional.of(new PaperBlockData(block.getBlockData()));
        return Optional.empty();
    }

    @Override
    public RBlock at(RWorld world, RBlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        World bukkitWorld = world.handle(World.class);
        return new PaperBlock(bukkitWorld.getBlockAt(pos.x(), pos.y(), pos.z()));
    }

    @Override
    public Optional<RBlockData> parseData(String value) {
        if (value == null) return Optional.empty();
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return Optional.empty();

        try {
            return Optional.of(new PaperBlockData(Bukkit.createBlockData(trimmed)));
        } catch (IllegalArgumentException ignored) {
            // try Material parsing next
        }

        Material material = Material.matchMaterial(trimmed);
        if (material == null) {
            try {
                material = Material.valueOf(trimmed.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        if (!material.isBlock()) return Optional.empty();
        return Optional.of(new PaperBlockData(material.createBlockData()));
    }
}

