package de.t14d3.rapunzellib.platform.velocity.objects;

import de.t14d3.rapunzellib.common.attachments.StoredPersistentAttachmentContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

final class VelocityPersistentAttachments extends StoredPersistentAttachmentContainer {
    VelocityPersistentAttachments(@NotNull VelocityPersistentAttachmentsStore store, @NotNull UUID ownerUuid) {
        super(
            () -> Objects.requireNonNull(store, "store").get(Objects.requireNonNull(ownerUuid, "ownerUuid")),
            root -> store.put(ownerUuid, root)
        );
    }
}
