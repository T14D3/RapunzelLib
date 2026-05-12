package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.item.DefaultItemStackAdapters;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapters;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Utility class providing helper methods for platform NBT feature installers.
 * <p>
 * Handles registration of {@link ItemStackAdapters} and individual {@link ItemStackAdapter}
 * instances into the given {@link RapunzelContext}.</p>
 */
public final class NbtFeatureInstallerSupport {
    private NbtFeatureInstallerSupport() {
    }

    /**
     * Returns (or creates) the {@link ItemStackAdapters} instance for the given context and platform.
     *
     * @param context    the Rapunzel context
     * @param platformId the platform ID
     * @return the item stack adapters
     */
    public static @NotNull ItemStackAdapters itemStackAdapters(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");

        return context.getOrCreate(ItemStackAdapters.class, () -> new DefaultItemStackAdapters(platformId));
    }

    /**
     * Registers an adapter as the primary (default) adapter for the given handle type.
     * The primary adapter is also registered under {@link ItemStackAdapter} as the default.
     *
     * @param <H>        the handle type
     * @param <A>        the adapter type
     * @param context    the Rapunzel context
     * @param platformId the platform ID
     * @param handleType the native handle class
     * @param adapterType the adapter class
     * @param adapter    the adapter instance
     * @return the registered adapter
     */
    public static <H, A extends ItemStackAdapter<? extends H>> @NotNull A registerPrimaryItemStackAdapter(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull Class<H> handleType,
        @NotNull Class<A> adapterType,
        @NotNull A adapter
    ) {
        registerItemStackAdapter(context, platformId, handleType, adapterType, adapter);
        context.register(ItemStackAdapter.class, adapter);
        return adapter;
    }

    /**
     * Registers a (non-primary) item stack adapter for the given handle type.
     *
     * @param <H>         the handle type
     * @param <A>         the adapter type
     * @param context     the Rapunzel context
     * @param platformId  the platform ID
     * @param handleType  the native handle class
     * @param adapterType the adapter class
     * @param adapter     the adapter instance
     * @return the registered adapter
     */
    public static <H, A extends ItemStackAdapter<? extends H>> @NotNull A registerItemStackAdapter(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull Class<H> handleType,
        @NotNull Class<A> adapterType,
        @NotNull A adapter
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(handleType, "handleType");
        Objects.requireNonNull(adapterType, "adapterType");
        Objects.requireNonNull(adapter, "adapter");

        DefaultItemStackAdapters adapters = (DefaultItemStackAdapters) itemStackAdapters(context, platformId);
        adapters.register(handleType, adapter);
        context.register(adapterType, adapter);
        return adapter;
    }
}
