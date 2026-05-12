package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A registry type representing a block type in the game.
 */
public interface RBlockType extends RRegistryType {
    /**
     * Creates a registry reference for the given block type key.
     *
     * @param key the block type key
     * @return the registry reference
     */
    static @NotNull RRegistryRef<RBlockType> ref(@NotNull RKey key) {
        return RRegistries.BLOCK_TYPES.ref(key);
    }

    /**
     * Creates a registry reference for the given block type key string.
     *
     * @param key the block type key string
     * @return the registry reference
     */
    static @NotNull RRegistryRef<RBlockType> ref(@NotNull String key) {
        return RRegistries.BLOCK_TYPES.ref(key);
    }

    /**
     * Finds a block type by key.
     *
     * @param key the block type key
     * @return an {@link Optional} containing the type, or empty if not found
     */
    static @NotNull Optional<RBlockType> find(@NotNull RKey key) {
        return ref(key).find();
    }

    /**
     * Finds a block type by string key.
     *
     * @param key the block type key string
     * @return an {@link Optional} containing the type, or empty if not found
     */
    static @NotNull Optional<RBlockType> find(@NotNull String key) {
        return ref(key).find();
    }

    /**
     * Requires a block type by key, throwing if not found.
     *
     * @param key the block type key
     * @return the block type
     */
    static @NotNull RBlockType require(@NotNull RKey key) {
        return ref(key).require();
    }

    /**
     * Requires a block type by string key, throwing if not found.
     *
     * @param key the block type key string
     * @return the block type
     */
    static @NotNull RBlockType require(@NotNull String key) {
        return ref(key).require();
    }
}
