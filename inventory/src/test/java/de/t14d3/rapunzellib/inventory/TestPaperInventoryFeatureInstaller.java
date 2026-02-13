package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TestPaperInventoryFeatureInstaller implements InventoryFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        TestInventoryFeatureInstallers.recordPaperInstall();
        InventoryFeatureInstallerSupport.registerInventories(context, PlatformId.PAPER, List.of(TestSupport.testFactory(PlatformId.PAPER)));
    }
}
