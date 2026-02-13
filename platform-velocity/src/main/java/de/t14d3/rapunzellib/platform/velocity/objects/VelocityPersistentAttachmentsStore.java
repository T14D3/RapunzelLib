package de.t14d3.rapunzellib.platform.velocity.objects;

import de.t14d3.rapunzellib.common.attachments.YamlPersistentAttachmentStore;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public final class VelocityPersistentAttachmentsStore implements AutoCloseable {
    private final YamlPersistentAttachmentStore store;

    public VelocityPersistentAttachmentsStore(@NotNull Logger logger, @NotNull ConfigService configService, @NotNull Path file) {
        this.store = new YamlPersistentAttachmentStore(
            Objects.requireNonNull(logger, "logger"),
            Objects.requireNonNull(configService, "configService"),
            Objects.requireNonNull(file, "file")
        );
    }

    public @NotNull RNbtCompound get(@NotNull UUID owner) {
        return store.get("players." + Objects.requireNonNull(owner, "owner"));
    }

    public void put(@NotNull UUID owner, @NotNull RNbtCompound root) {
        store.put("players." + Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(root, "root"));
    }

    @Override
    public void close() {
        store.close();
    }
}
