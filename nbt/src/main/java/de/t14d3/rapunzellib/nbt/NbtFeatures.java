package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapters;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Lazily installs the platform NBT services exposed through the active {@link RapunzelContext}.
 */
public final class NbtFeatures {
    private static final FeatureInstallerRegistry<NbtFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        NbtFeatureInstaller.class,
        NbtFeatureInstaller::platformId,
        "rapunzellib-nbt-"
    );

    private NbtFeatures() {
    }

    public static @NotNull ItemStackAdapters install() {
        return install(Rapunzel.context());
    }

    public static @NotNull ItemStackAdapters install(@NotNull RapunzelContext context) {
        return FeatureInstallationSupport.install(
            context,
            ItemStackAdapters.class,
            RuntimeCapability.NBT,
            "NBT features",
            () -> INSTALLER_REGISTRY.resolve(context.platformId()).install(context)
        );
    }

    public static @NotNull ItemStackAdapters itemStacks() {
        return install();
    }

    public static <T> @NotNull ItemStackAdapter<T> itemStackAdapter(@NotNull Class<T> handleType) {
        return itemStacks().require(handleType);
    }

    /**
     * Returns the platform serializer for live entities/items.
     */
    @SuppressWarnings("unchecked")
    public static <E, L> @NotNull NbtSerializer<E, L> serializer() {
        install();
        return (NbtSerializer<E, L>) Rapunzel.context().services().get(NbtSerializer.class);
    }

    /**
     * Returns the platform block-entity serializer when the active platform registers one.
     *
     * <p>Platforms that only support item/entity NBT may leave this service absent.</p>
     */
    @SuppressWarnings("unchecked")
    public static <B, L> @NotNull BlockEntityNbtSerializer<B, L> blockEntitySerializer() {
        install();
        return (BlockEntityNbtSerializer<B, L>) Rapunzel.context().services().get(BlockEntityNbtSerializer.class);
    }
}
