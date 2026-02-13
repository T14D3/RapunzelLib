package de.t14d3.rapunzellib.nbt.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.nbt.shared.AbstractSharedItemStackAdapter;

public class NeoForgeItemStackAdapter extends AbstractSharedItemStackAdapter {
    public NeoForgeItemStackAdapter() {
        super(PlatformId.NEOFORGE);
    }
}
