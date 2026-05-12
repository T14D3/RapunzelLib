package de.t14d3.rapunzellib.common.attachments;

import de.t14d3.rapunzellib.attachments.AttachmentStorageSupport;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default attachment container backed by simple load/save callbacks for a persistent root compound.
 * <p>
 * Uses a {@link Supplier} for loading and a {@link Consumer} for saving the persistent NBT root,
 * wrapping them into a {@link PersistentAttachmentSession} on each access.
 */
public class StoredPersistentAttachmentContainer extends DefaultAttachmentContainer {
    /** Loads the current persistent root compound */
    private final Supplier<RNbtCompound> loader;
    /** Saves an updated persistent root compound */
    private final Consumer<RNbtCompound> saver;

    /**
     * Creates a container with full transient and persistent support.
     *
     * @param loader loads the persistent root compound
     * @param saver  saves the persistent root compound
     */
    public StoredPersistentAttachmentContainer(
        @NotNull Supplier<@NotNull RNbtCompound> loader,
        @NotNull Consumer<@NotNull RNbtCompound> saver
    ) {
        this(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, loader, saver);
    }

    /**
     * Creates a container with the given storage support.
     *
     * @param support the attachment storage support
     * @param loader  loads the persistent root compound
     * @param saver   saves the persistent root compound
     */
    public StoredPersistentAttachmentContainer(
        @NotNull AttachmentStorageSupport support,
        @NotNull Supplier<@NotNull RNbtCompound> loader,
        @NotNull Consumer<@NotNull RNbtCompound> saver
    ) {
        super(support);
        this.loader = Objects.requireNonNull(loader, "loader");
        this.saver = Objects.requireNonNull(saver, "saver");
    }

    /**
     * Opens a session backed by the configured loader and saver.
     *
     * @return a new persistent attachment session
     */
    @Override
    protected @NotNull PersistentAttachmentSession openSession() {
        return PersistentAttachmentSession.of(loader, saver);
    }
}
