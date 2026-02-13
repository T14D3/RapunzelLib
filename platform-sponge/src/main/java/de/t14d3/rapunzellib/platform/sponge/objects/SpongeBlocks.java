package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.objects.BlockCacheKey;
import de.t14d3.rapunzellib.common.objects.KeyedLruCache;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryHandles;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;
import java.util.Optional;

public final class SpongeBlocks implements Blocks {
    private static final int CACHE_SIZE = 10_000;

    private final KeyedLruCache<BlockCacheKey, SpongeBlock> blockCache = new KeyedLruCache<>(CACHE_SIZE);
    private final SpongeAttachmentService attachmentService;
    private final SpongeWorlds worlds;

    public SpongeBlocks(@NotNull SpongeAttachmentService attachmentService, @NotNull SpongeWorlds worlds) {
        this.attachmentService = attachmentService;
        this.worlds = worlds;
    }

    @Override
    public @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
        switch (nativeBlock) {
            case ServerLocation loc -> {
                return wrap(loc).map(RBlock.class::cast);
            }
            case LocatableBlock locatable -> {
                return wrap(locatable).map(RBlock.class::cast);
            }
            case BlockSnapshot snapshot -> {
                return wrap(snapshot).map(RBlock.class::cast);
            }
            default -> {
            }
        }
        return Optional.empty();
    }

    public @NotNull Optional<SpongeBlock> wrap(@NotNull ServerLocation location) {
        return Optional.of(wrapInternal(location.world(), location.blockPosition()));
    }

    public @NotNull Optional<SpongeBlock> wrap(@NotNull LocatableBlock locatable) {
        if (!(locatable.world() instanceof ServerWorld world)) return Optional.empty();
        return Optional.of(wrapInternal(world, locatable.blockPosition()));
    }

    public @NotNull Optional<SpongeBlock> wrap(@NotNull BlockSnapshot snapshot) {
        if (!Sponge.isServerAvailable()) return Optional.empty();
        Vector3i pos = snapshot.position();
        return Sponge.server().worldManager().world(snapshot.world()).map(world -> wrapInternal(world, pos));
    }

    public @NotNull SpongeBlock wrapNative(@NotNull ServerWorld world, @NotNull Vector3i pos) {
        return wrapInternal(world, pos);
    }

    private SpongeBlock wrapInternal(ServerWorld world, Vector3i pos) {
        BlockCacheKey key = BlockCacheKey.of(world.key().asString(), pos.x(), pos.y(), pos.z());
        return blockCache.getOrCreate(key, ignored -> new SpongeBlock(world, pos, attachmentService, worlds));
    }

    @Override
    public @NotNull Optional<RBlockData> wrapData(@NotNull Object nativeBlockData) {
        return switch (nativeBlockData) {
            case BlockState state -> Optional.of(new SpongeBlockData(state));
            case BlockType type -> Optional.of(new SpongeBlockData(type.defaultState()));
            case BlockSnapshot snapshot -> Optional.of(new SpongeBlockData(snapshot.state()));
            case LocatableBlock locatable -> Optional.of(new SpongeBlockData(locatable.blockState()));
            case ServerLocation loc -> Optional.of(new SpongeBlockData(loc.block()));
            default -> Optional.empty();
        };
    }

    @Override
    public @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        ServerWorld spongeWorld = world.handle(ServerWorld.class);
        return wrapInternal(spongeWorld, new Vector3i(pos.x(), pos.y(), pos.z()));
    }

    @Override
    public @NotNull Optional<RBlockData> parseData(@NotNull String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) return Optional.empty();

        RKey typeKey = parseTypeKey(trimmed).orElse(null);
        if (typeKey == null) return Optional.empty();

        BlockType blockType = RRegistryHandles.find(RBlockType.ref(typeKey), BlockType.class)
            .or(() -> findNativeBlockType(typeKey))
            .orElse(null);
        if (blockType == null) return Optional.empty();

        if (trimmed.indexOf('[') < 0) {
            return Optional.of(new SpongeBlockData(blockType.defaultState()));
        }

        try {
            return Optional.of(new SpongeBlockData(BlockState.fromString(trimmed)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static @NotNull Optional<RKey> parseTypeKey(@NotNull String value) {
        int bracketIndex = value.indexOf('[');
        String id = (bracketIndex >= 0 ? value.substring(0, bracketIndex) : value).trim();
        if (id.isEmpty()) return Optional.empty();
        return id.contains(":") ? RKey.tryParse(id) : Optional.of(RKey.of("minecraft", id));
    }

    private static @NotNull Optional<BlockType> findNativeBlockType(@NotNull RKey typeKey) {
        try {
            return Sponge.server().registry(RegistryTypes.BLOCK_TYPE)
                .findValue(org.spongepowered.api.ResourceKey.resolve(typeKey.asString()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
