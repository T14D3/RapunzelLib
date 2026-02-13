package de.t14d3.rapunzellib.nbt.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.nbt.shared.AbstractSharedNbtFeatureInstaller;

public final class NeoForgeNbtFeatureInstaller extends AbstractSharedNbtFeatureInstaller<NeoForgeItemStackAdapter> {
    public NeoForgeNbtFeatureInstaller() {
        super(PlatformId.NEOFORGE, NeoForgeItemStackAdapter.class, NeoForgeItemStackAdapter::new);
    }
}
