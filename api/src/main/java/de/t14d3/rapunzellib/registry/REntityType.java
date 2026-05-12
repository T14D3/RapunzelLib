package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A registry type representing an entity type in the game.
 */
public interface REntityType extends RRegistryType {
    /**
     * Creates a registry reference for the given entity type key.
     *
     * @param key the entity type key
     * @return the registry reference
     */
    static @NotNull RRegistryRef<REntityType> ref(@NotNull RKey key) {
        return RRegistries.ENTITY_TYPES.ref(key);
    }

    /**
     * Creates a registry reference for the given entity type key string.
     *
     * @param key the entity type key string
     * @return the registry reference
     */
    static @NotNull RRegistryRef<REntityType> ref(@NotNull String key) {
        return RRegistries.ENTITY_TYPES.ref(key);
    }

    /**
     * Finds an entity type by key.
     *
     * @param key the entity type key
     * @return an {@link Optional} containing the type, or empty if not found
     */
    static @NotNull Optional<REntityType> find(@NotNull RKey key) {
        return ref(key).find();
    }

    /**
     * Finds an entity type by string key.
     *
     * @param key the entity type key string
     * @return an {@link Optional} containing the type, or empty if not found
     */
    static @NotNull Optional<REntityType> find(@NotNull String key) {
        return ref(key).find();
    }

    /**
     * Requires an entity type by key, throwing if not found.
     *
     * @param key the entity type key
     * @return the entity type
     */
    static @NotNull REntityType require(@NotNull RKey key) {
        return ref(key).require();
    }

    /**
     * Requires an entity type by string key, throwing if not found.
     *
     * @param key the entity type key string
     * @return the entity type
     */
    static @NotNull REntityType require(@NotNull String key) {
        return ref(key).require();
    }
}
