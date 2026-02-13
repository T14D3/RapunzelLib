package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RItemType extends RRegistryType {
    static @NotNull RRegistryRef<RItemType> ref(@NotNull RKey key) {
        return RRegistries.ITEM_TYPES.ref(key);
    }

    static @NotNull RRegistryRef<RItemType> ref(@NotNull String key) {
        return RRegistries.ITEM_TYPES.ref(key);
    }

    static @NotNull Optional<RItemType> find(@NotNull RKey key) {
        return ref(key).find();
    }

    static @NotNull Optional<RItemType> find(@NotNull String key) {
        return ref(key).find();
    }

    static @NotNull RItemType require(@NotNull RKey key) {
        return ref(key).require();
    }

    static @NotNull RItemType require(@NotNull String key) {
        return ref(key).require();
    }
}
