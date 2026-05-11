package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.BlockEntityNbtSerializer;
import de.t14d3.rapunzellib.nbt.NbtFeatureInstallerSupport;
import de.t14d3.rapunzellib.nbt.NbtSerializer;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.NativeRItemFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SharedNbtFeatureInstallerSupport {
    private SharedNbtFeatureInstallerSupport() {
    }

    public static <A extends ItemStackAdapter<net.minecraft.world.item.ItemStack>> void install(
        @NotNull RapunzelContext context,
        @NotNull de.t14d3.rapunzellib.PlatformId platformId,
        @NotNull Class<A> adapterType,
        @NotNull A adapter
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(adapterType, "adapterType");
        Objects.requireNonNull(adapter, "adapter");

        NbtFeatureInstallerSupport.registerPrimaryItemStackAdapter(
            context,
            platformId,
            net.minecraft.world.item.ItemStack.class,
            adapterType,
            adapter
        );

        if (adapter instanceof AbstractSharedItemStackAdapter sharedAdapter) {
            context.register(NativeRItemFactory.class, sharedAdapter.factory());
        }

        registerSerializers(context);
    }

    public static void registerSerializers(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");
        registerEntitySerializer(context);
        registerBlockEntitySerializer(context);
    }

    public static void registerEntitySerializer(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");

        context.register(
            NbtSerializer.class,
            new SharedEntityNbtSerializerCore<>()
        );
    }

    public static void registerBlockEntitySerializer(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");

        context.register(
            BlockEntityNbtSerializer.class,
            new SharedBlockEntityNbtSerializerCore<>()
        );
    }
}
