package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RRegistryType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import de.t14d3.rapunzellib.registry.RTypeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractTypeRegistry<H, W extends RRegistryTypeHandle<H>, V extends RRegistryType>
    extends CachedRegistryWrappers<H, W>
    implements RTypeRegistry<V> {
    private final Function<? super RKey, ? extends H> handleLookup;
    private final Supplier<? extends Iterable<? extends H>> entriesSupplier;
    private final Function<? super H, RKey> keyResolver;
    private final Class<V> viewType;

    protected AbstractTypeRegistry(
        @NotNull Function<? super RKey, ? extends H> handleLookup,
        @NotNull Supplier<? extends Iterable<? extends H>> entriesSupplier,
        @NotNull Function<? super H, RKey> keyResolver,
        @NotNull Class<V> viewType
    ) {
        this.handleLookup = Objects.requireNonNull(handleLookup, "handleLookup");
        this.entriesSupplier = Objects.requireNonNull(entriesSupplier, "entriesSupplier");
        this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver");
        this.viewType = Objects.requireNonNull(viewType, "viewType");
    }

    public final @NotNull Optional<V> find(@NotNull RKey key) {
        RKey requestedKey = Objects.requireNonNull(key, "key");
        return findWrapped(requestedKey, handleLookup.apply(requestedKey), keyResolver).map(viewType::cast);
    }

    public final @NotNull List<V> entries() {
        return wrapEntries(entriesSupplier.get(), keyResolver, viewType);
    }
}
