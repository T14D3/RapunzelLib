package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.NbtFeatureInstaller;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractSharedNbtFeatureInstaller<A extends ItemStackAdapter<ItemStack>> implements NbtFeatureInstaller {
    private final PlatformId platformId;
    private final Class<A> adapterType;
    private final Supplier<? extends A> adapterFactory;

    protected AbstractSharedNbtFeatureInstaller(
        @NotNull PlatformId platformId,
        @NotNull Class<A> adapterType,
        @NotNull Supplier<? extends A> adapterFactory
    ) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.adapterType = Objects.requireNonNull(adapterType, "adapterType");
        this.adapterFactory = Objects.requireNonNull(adapterFactory, "adapterFactory");
    }

    @Override
    public final @NotNull PlatformId platformId() {
        return platformId;
    }

    @Override
    public final void install(@NotNull RapunzelContext context) {
        SharedNbtFeatureInstallerSupport.install(context, platformId, adapterType, adapterFactory.get());
    }
}
