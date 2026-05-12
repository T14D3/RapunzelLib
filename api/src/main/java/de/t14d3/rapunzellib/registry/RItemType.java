package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A registry type representing an item type in the game.
 */
public interface RItemType extends RRegistryType {
    /**
     * Creates a registry reference for the given item type key.
     *
     * @param key the item type key
     * @return the registry reference
     */
    static @NotNull RRegistryRef<RItemType> ref(@NotNull RKey key) {
        return RRegistries.ITEM_TYPES.ref(key);
    }

    /**
     * Creates a registry reference for the given item type key string.
     *
     * @param key the item type key string
     * @return the registry reference
     */
    static @NotNull RRegistryRef<RItemType> ref(@NotNull String key) {
        return RRegistries.ITEM_TYPES.ref(key);
    }

    /**
     * Finds an item type by key.
     *
     * @param key the item type key
     * @return an {@link Optional} containing the type, or empty if not found
     */
    static @NotNull Optional<RItemType> find(@NotNull RKey key) {
        return ref(key).find();
    }

    /**
     * Finds an item type by string key.
     *
     * @param key the item type key string
     * @return an {@link Optional} containing the type, or empty if not found
     */
    static @NotNull Optional<RItemType> find(@NotNull String key) {
        return ref(key).find();
    }

    /**
     * Requires an item type by key, throwing if not found.
     *
     * @param key the item type key
     * @return the item type
     */
    static @NotNull RItemType require(@NotNull RKey key) {
        return ref(key).require();
    }

    /**
     * Requires an item type by string key, throwing if not found.
     *
     * @param key the item type key string
     * @return the item type
     */
    static @NotNull RItemType require(@NotNull String key) {
        return ref(key).require();
    }
}
