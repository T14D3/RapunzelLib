package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.item.DefaultItemStackAdapters;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapters;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class NbtFeatureInstallerSupport {
    private NbtFeatureInstallerSupport() {
    }

    public static @NotNull ItemStackAdapters itemStackAdapters(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");

        return context.getOrCreate(ItemStackAdapters.class, () -> new DefaultItemStackAdapters(platformId));
    }

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
