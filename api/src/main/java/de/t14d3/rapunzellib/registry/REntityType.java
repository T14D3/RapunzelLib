package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface REntityType extends RRegistryType {
    static @NotNull RRegistryRef<REntityType> ref(@NotNull RKey key) {
        return RRegistries.ENTITY_TYPES.ref(key);
    }

    static @NotNull RRegistryRef<REntityType> ref(@NotNull String key) {
        return RRegistries.ENTITY_TYPES.ref(key);
    }

    static @NotNull Optional<REntityType> find(@NotNull RKey key) {
        return ref(key).find();
    }

    static @NotNull Optional<REntityType> find(@NotNull String key) {
        return ref(key).find();
    }

    static @NotNull REntityType require(@NotNull RKey key) {
        return ref(key).require();
    }

    static @NotNull REntityType require(@NotNull String key) {
        return ref(key).require();
    }
}
