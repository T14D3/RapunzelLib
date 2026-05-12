package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A registry for {@link RRegistryType} values, where keys are derived from the type values themselves.
 *
 * @param <T> the registry type value
 */
public interface RTypeRegistry<T extends RRegistryType> extends RRegistry<T> {
    @Override
    default @NotNull List<RKey> keys() {
        return entries().stream().map(RRegistryType::key).toList();
    }
}
