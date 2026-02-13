package de.t14d3.rapunzellib.platform.sponge.attachments;

import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.common.attachments.StoredPersistentAttachmentContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SpongeAttachmentService {
    private final SpongePersistentAttachmentsStore store;
    private final ConcurrentHashMap<String, RAttachmentContainer> containers = new ConcurrentHashMap<>();

    public SpongeAttachmentService(@NotNull SpongePersistentAttachmentsStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public @NotNull RAttachmentContainer forPlayer(@NotNull ServerPlayer player) {
        return container("player:" + player.profile().uuid(), "players", player.profile().uuid().toString());
    }

    public @NotNull RAttachmentContainer forEntity(@NotNull Entity entity) {
        return container("entity:" + entity.uniqueId(), "entities", entity.uniqueId().toString());
    }

    public @NotNull RAttachmentContainer forWorld(@NotNull ServerWorld world) {
        return container("world:" + world.key().asString(), "worlds", world.key().asString());
    }

    public @NotNull RAttachmentContainer forBlock(@NotNull ServerWorld world, @NotNull Vector3i pos) {
        String id = world.key().asString() + ":" + pos.x() + ":" + pos.y() + ":" + pos.z();
        return container("block:" + id, "blocks", id);
    }

    private @NotNull RAttachmentContainer container(@NotNull String cacheKey, @NotNull String category, @NotNull String id) {
        return containers.computeIfAbsent(
            cacheKey,
            ignored -> new StoredPersistentAttachmentContainer(() -> store.get(category, id), root -> store.put(category, id, root))
        );
    }
}
