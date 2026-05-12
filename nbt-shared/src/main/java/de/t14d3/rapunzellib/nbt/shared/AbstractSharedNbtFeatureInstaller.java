package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.NbtFeatureInstaller;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Abstract base for platform-specific NBT feature installers.
 * <p>
 * Delegates registration of the primary item stack adapter and NBT
 * serializers to {@link SharedNbtFeatureInstallerSupport}.
 *
 * @param <A> the concrete item stack adapter type
 */
public abstract class AbstractSharedNbtFeatureInstaller<A extends ItemStackAdapter<ItemStack>> implements NbtFeatureInstaller {
    private final PlatformId platformId;
    private final Class<A> adapterType;
    private final Supplier<? extends A> adapterFactory;

    /**
     * Creates an NBT feature installer.
     *
     * @param platformId     the platform identifier
     * @param adapterType    the adapter class
     * @param adapterFactory the adapter factory
     */
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
