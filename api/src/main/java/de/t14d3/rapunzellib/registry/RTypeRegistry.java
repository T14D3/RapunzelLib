package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface RTypeRegistry<T extends RRegistryType> extends RRegistry<T> {
    default @NotNull List<RKey> keys() {
        return entries().stream().map(RRegistryType::key).toList();
    }
}
