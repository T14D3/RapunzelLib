package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RBlockType extends RRegistryType {
    static @NotNull RRegistryRef<RBlockType> ref(@NotNull RKey key) {
        return RRegistries.BLOCK_TYPES.ref(key);
    }

    static @NotNull RRegistryRef<RBlockType> ref(@NotNull String key) {
        return RRegistries.BLOCK_TYPES.ref(key);
    }

    static @NotNull Optional<RBlockType> find(@NotNull RKey key) {
        return ref(key).find();
    }

    static @NotNull Optional<RBlockType> find(@NotNull String key) {
        return ref(key).find();
    }

    static @NotNull RBlockType require(@NotNull RKey key) {
        return ref(key).require();
    }

    static @NotNull RBlockType require(@NotNull String key) {
        return ref(key).require();
    }
}
