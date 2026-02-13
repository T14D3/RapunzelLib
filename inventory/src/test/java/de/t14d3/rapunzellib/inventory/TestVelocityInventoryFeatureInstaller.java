package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TestVelocityInventoryFeatureInstaller implements InventoryFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.VELOCITY;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        TestInventoryFeatureInstallers.recordVelocityInstall();
        InventoryFeatureInstallerSupport.registerInventories(context, PlatformId.VELOCITY, List.of(TestSupport.testFactory(PlatformId.VELOCITY)));
    }
}
