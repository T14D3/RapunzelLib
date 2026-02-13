package de.t14d3.rapunzellib.common.attachments;

import de.t14d3.rapunzellib.attachments.AttachmentStorageSupport;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default attachment container backed by simple load/save callbacks for a persistent root compound.
 */
public class StoredPersistentAttachmentContainer extends DefaultAttachmentContainer {
    private final Supplier<RNbtCompound> loader;
    private final Consumer<RNbtCompound> saver;

    public StoredPersistentAttachmentContainer(
        @NotNull Supplier<@NotNull RNbtCompound> loader,
        @NotNull Consumer<@NotNull RNbtCompound> saver
    ) {
        this(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, loader, saver);
    }

    public StoredPersistentAttachmentContainer(
        @NotNull AttachmentStorageSupport support,
        @NotNull Supplier<@NotNull RNbtCompound> loader,
        @NotNull Consumer<@NotNull RNbtCompound> saver
    ) {
        super(support);
        this.loader = Objects.requireNonNull(loader, "loader");
        this.saver = Objects.requireNonNull(saver, "saver");
    }

    @Override
    protected @NotNull PersistentAttachmentSession openSession() {
        return PersistentAttachmentSession.of(loader, saver);
    }
}
