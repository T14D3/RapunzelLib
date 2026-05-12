package de.t14d3.rapunzellib.common.attachments;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Read/write view over the persistent attachment payload for a single native owner.
 *
 * <p>Implementations are deliberately small. They only need to materialize the current root compound
 * and persist an updated root when the container mutates a persistent attachment.</p>
 */
public interface PersistentAttachmentSession {
    /**
     * Loads the current persistent root compound.
     *
     * @return the root NBT compound
     */
    @NotNull RNbtCompound load();

    /**
     * Persists an updated root compound.
     *
     * @param root the updated root NBT compound
     */
    void save(@NotNull RNbtCompound root);

    /**
     * Creates a session from loader and saver callbacks.
     *
     * @param loader supplies the current root compound
     * @param saver  accepts an updated root compound for persistence
     * @return a new session instance
     */
    static @NotNull PersistentAttachmentSession of(
        @NotNull Supplier<@NotNull RNbtCompound> loader,
        @NotNull Consumer<@NotNull RNbtCompound> saver
    ) {
        Supplier<RNbtCompound> resolvedLoader = Objects.requireNonNull(loader, "loader");
        Consumer<RNbtCompound> resolvedSaver = Objects.requireNonNull(saver, "saver");
        return new PersistentAttachmentSession() {
            @Override
            public @NotNull RNbtCompound load() {
                return resolvedLoader.get();
            }

            @Override
            public void save(@NotNull RNbtCompound root) {
                resolvedSaver.accept(Objects.requireNonNull(root, "root"));
            }
        };
    }
}
