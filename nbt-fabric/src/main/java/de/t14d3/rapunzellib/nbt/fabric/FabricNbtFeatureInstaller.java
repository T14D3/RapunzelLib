package de.t14d3.rapunzellib.nbt.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.nbt.shared.AbstractSharedNbtFeatureInstaller;

public final class FabricNbtFeatureInstaller extends AbstractSharedNbtFeatureInstaller<FabricItemStackAdapter> {
    public FabricNbtFeatureInstaller() {
        super(PlatformId.FABRIC, FabricItemStackAdapter.class, FabricItemStackAdapter::new);
    }
}
