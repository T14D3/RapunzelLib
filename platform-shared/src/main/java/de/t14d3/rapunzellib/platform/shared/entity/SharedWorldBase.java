package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Abstract base implementation of {@link RWorld}, wrapping a Minecraft {@link ServerLevel}. */
public abstract class SharedWorldBase extends RNativeHandle<ServerLevel> implements RWorld {
    private final SharedWorldHooks worldHooks;

    protected SharedWorldBase(@NotNull PlatformId platformId, @NotNull ServerLevel world) {
        this(platformId, world, RAttachmentContainer.lazyMutable(), SharedWorldHooks.unsupported());
    }

    protected SharedWorldBase(
        @NotNull PlatformId platformId,
        @NotNull ServerLevel world,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, Objects.requireNonNull(world, "world"), Objects.requireNonNull(attachments, "attachments"));
        this.worldHooks = Objects.requireNonNull(worldHooks, "worldHooks");
    }

    @Override
    public final @NotNull RWorldRef ref() {
        return worldHooks.worldRef(handle());
    }

    @Override
    public final @NotNull Optional<UUID> uuid() {
        return worldHooks.worldUuid(handle());
    }

    @Override
    public final boolean canSpawnEntities() {
        return true;
    }

    @Override
    public final @NotNull Optional<REntity> spawn(@NotNull RRegistryRef<REntityType> type, @NotNull RLocation location) {
        return SharedEntityOperations.spawn(handle(), type, location, worldHooks);
    }

    /**
     * Bulk-fills a rectangular volume with the given block data.
     * <p>
     * Overrides the default {@link RWorld#fill(RBlockPos, RBlockPos, RBlockData)} to
     * set blocks directly on the native {@link ServerLevel} with physics suppressed
     * ({@link Block#UPDATE_CLIENTS} only). This avoids per-block wrapper creation,
     * LRU cache lookups, and neighbor physics cascades, making large fills
     * significantly faster and less intrusive.
     * </p>
     */
    @Override
    public void fill(@NotNull RBlockPos min, @NotNull RBlockPos max, @NotNull RBlockData data) {
        int minX = Math.min(min.x(), max.x()), maxX = Math.max(min.x(), max.x());
        int minY = Math.min(min.y(), max.y()), maxY = Math.max(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z()), maxZ = Math.max(min.z(), max.z());
        ServerLevel level = handle();
        BlockState state = (BlockState) data.handle();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            cursor.setX(x);
            for (int y = minY; y <= maxY; y++) {
                cursor.setY(y);
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.setZ(z);
                    level.setBlock(cursor, state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
