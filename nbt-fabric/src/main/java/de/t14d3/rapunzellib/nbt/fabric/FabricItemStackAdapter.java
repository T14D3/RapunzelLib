package de.t14d3.rapunzellib.nbt.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.nbt.shared.AbstractSharedItemStackAdapter;

public class FabricItemStackAdapter extends AbstractSharedItemStackAdapter {
    public FabricItemStackAdapter() {
        super(PlatformId.FABRIC);
    }
}
