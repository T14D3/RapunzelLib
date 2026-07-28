package de.t14d3.rapunzellib.platform.shared.attachments;

import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.common.attachments.StoredPersistentAttachmentContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared attachment service providing per-player, per-entity, per-world, and
 * per-block attachment containers backed by a persistent YAML store.
 *
 * <p>Used by Fabric and NeoForge (and any other VanillaGradle-based platform).
 * The dimension key extraction uses a version-conditional to handle the API
 * rename from {@code location()} to {@code identifier()} in Minecraft 26+.</p>
 */
public class SharedAttachmentService {
    private final SharedPersistentAttachmentsStore store;
    private final ConcurrentHashMap<String, RAttachmentContainer> containers = new ConcurrentHashMap<>();

    public SharedAttachmentService(@NotNull SharedPersistentAttachmentsStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public @NotNull RAttachmentContainer forPlayer(@NotNull ServerPlayer player) {
        return container("player:" + player.getUUID(), "players", player.getUUID().toString());
    }

    public @NotNull RAttachmentContainer forEntity(@NotNull Entity entity) {
        return container("entity:" + entity.getUUID(), "entities", entity.getUUID().toString());
    }

    public @NotNull RAttachmentContainer forWorld(@NotNull ServerLevel level) {
        return container("world:" + dimensionKey(level), "worlds", dimensionKey(level));
    }

    public @NotNull RAttachmentContainer forBlock(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        String id = dimensionKey(level) + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
        return container("block:" + id, "blocks", id);
    }

    private @NotNull RAttachmentContainer container(@NotNull String cacheKey, @NotNull String category, @NotNull String id) {
        return containers.computeIfAbsent(
            cacheKey,
            ignored -> new StoredPersistentAttachmentContainer(
                () -> store.get(category, id),
                root -> store.put(category, id, root)
            )
        );
    }

    private static @NotNull String dimensionKey(@NotNull ServerLevel level) {
        // #if VERSION >= 1.21.11
        return level.dimension().identifier().toString();
        // #else
        return level.dimension().location().toString();
        // #endif
    }
}