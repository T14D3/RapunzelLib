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

/**
 * Abstract base for type registries that wrap native handles into view types.
 * <p>
 * Extends {@link CachedRegistryWrappers} to provide cached lookup by key and
 * enumeration of all entries, using a handle lookup function and a key resolver.
 *
 * @param <H> the native handle type
 * @param <W> the wrapper type
 * @param <V> the view type extending {@link RRegistryType}
 */
public abstract class AbstractTypeRegistry<H, W extends RRegistryTypeHandle<H>, V extends RRegistryType>
    extends CachedRegistryWrappers<H, W>
    implements RTypeRegistry<V> {
    private final Function<? super RKey, ? extends H> handleLookup;
    private final Supplier<? extends Iterable<? extends H>> entriesSupplier;
    private final Function<? super H, RKey> keyResolver;
    private final Class<V> viewType;

    /**
     * Creates an abstract type registry.
     *
     * @param handleLookup    function to look up a handle by key
     * @param entriesSupplier supplier of all handles
     * @param keyResolver     function to extract the key from a handle
     * @param viewType        the view type class
     */
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

    /**
     * Finds a typed registry entry by key.
     *
     * @param key the lookup key
     * @return an optional containing the entry, or empty if not found
     */
    public final @NotNull Optional<V> find(@NotNull RKey key) {
        RKey requestedKey = Objects.requireNonNull(key, "key");
        return findWrapped(requestedKey, handleLookup.apply(requestedKey), keyResolver).map(viewType::cast);
    }

    /**
     * Returns all entries in this type registry.
     *
     * @return an immutable list of entries
     */
    public final @NotNull List<V> entries() {
        return wrapEntries(entriesSupplier.get(), keyResolver, viewType);
    }
}
