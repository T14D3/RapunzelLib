package de.t14d3.rapunzellib.platform.shared.attachments;

import de.t14d3.rapunzellib.common.attachments.YamlPersistentAttachmentStore;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

/**
 * Shared YAML-backed persistent attachment store used by Fabric and NeoForge
 * (and potentially other VanillaGradle-based platforms).
 *
 * <p>Delegates all storage to {@link YamlPersistentAttachmentStore}. Keys are
 * namespaced under {@code attachments.<category>.<base64(id)>}.</p>
 */
public class SharedPersistentAttachmentsStore implements AutoCloseable {
    private final YamlPersistentAttachmentStore store;

    public SharedPersistentAttachmentsStore(@NotNull Logger logger, @NotNull ConfigService configService, @NotNull Path file) {
        this.store = new YamlPersistentAttachmentStore(
            Objects.requireNonNull(logger, "logger"),
            Objects.requireNonNull(configService, "configService"),
            Objects.requireNonNull(file, "file")
        );
    }

    public @NotNull RNbtCompound get(@NotNull String category, @NotNull String id) {
        return store.get(path(Objects.requireNonNull(category, "category"), Objects.requireNonNull(id, "id")));
    }

    public void put(@NotNull String category, @NotNull String id, @NotNull RNbtCompound root) {
        store.put(path(Objects.requireNonNull(category, "category"), Objects.requireNonNull(id, "id")), Objects.requireNonNull(root, "root"));
    }

    @Override
    public void close() {
        store.close();
    }

    private static @NotNull String path(@NotNull String category, @NotNull String id) {
        return "attachments." + category + '.' + normalize(id);
    }

    private static @NotNull String normalize(@NotNull String id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(StandardCharsets.UTF_8));
    }
}