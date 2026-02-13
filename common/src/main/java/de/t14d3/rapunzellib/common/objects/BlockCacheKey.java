package de.t14d3.rapunzellib.common.objects;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record BlockCacheKey(@NotNull String worldKey, int x, int y, int z) {
    public BlockCacheKey {
        worldKey = Objects.requireNonNull(worldKey, "worldKey");
    }

    public static @NotNull BlockCacheKey of(@NotNull RKey worldKey, int x, int y, int z) {
        return new BlockCacheKey(worldKey.asString(), x, y, z);
    }

    public static @NotNull BlockCacheKey of(@NotNull String worldKey, int x, int y, int z) {
        return new BlockCacheKey(worldKey, x, y, z);
    }
}
