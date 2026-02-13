package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Live server-thread world wrapper.
 */
public interface RWorld extends RNative {
    @NotNull RWorldRef ref();

    default @NotNull RKey key() {
        return Objects.requireNonNull(ref().key(), "World does not expose a key");
    }

    default @NotNull Optional<UUID> uuid() {
        return Optional.empty();
    }

    /**
     * Returns whether raw entity spawning semantics are available for this live world.
     */
    default boolean canSpawnEntities() {
        return false;
    }

    /**
     * Spawns a live entity using generic platform spawn semantics.
     *
     * <p>This is intentionally a thin raw action: it does not model causes or platform-specific
     * spawn options. Callers needing async-safe data should snapshot the returned entity.</p>
     */
    default @NotNull Optional<REntity> spawn(@NotNull RRegistryRef<REntityType> type, @NotNull RLocation location) {
        return Optional.empty();
    }

    default @NotNull Optional<REntity> spawn(@NotNull REntityType type, @NotNull RLocation location) {
        return spawn(REntityType.ref(type.key()), location);
    }

    default @NotNull Optional<REntity> spawn(@NotNull RKey typeKey, @NotNull RLocation location) {
        return spawn(REntityType.ref(typeKey), location);
    }

    default @NotNull Optional<REntity> spawn(@NotNull String typeKey, @NotNull RLocation location) {
        return spawn(REntityType.ref(typeKey), location);
    }

    static @NotNull Collection<RWorld> all() {
        return Rapunzel.worlds().all();
    }

    static @NotNull Optional<RWorld> getByName(@NotNull String name) {
        return Rapunzel.worlds().getByName(name);
    }

    static @NotNull Optional<RWorld> get(@NotNull RKey key) {
        return Rapunzel.worlds().get(key);
    }

    static @NotNull Optional<RWorld> get(@NotNull String key) {
        return Rapunzel.worlds().get(key);
    }

    static @NotNull RWorld require(@NotNull RKey key) {
        return Rapunzel.worlds().require(key);
    }

    static @NotNull RWorld require(@NotNull String key) {
        return Rapunzel.worlds().require(key);
    }

    static @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        return Rapunzel.worlds().wrap(nativeWorld);
    }
}
