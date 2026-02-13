package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;
import org.jetbrains.annotations.NotNull;

public final class TestInventoryGameEventSupportContributor implements GameEventSupportContributor {
    static final GameEventSupportManifest MANIFEST = GameEventSupportManifests.withGuiInventoryBridgeSupport(
        GameEventSupportManifest.builder(PlatformId.PAPER)
            .emulatedSupport("contributor-should-not-downgrade-native", BlockBreakPre.class)
    ).build();

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public @NotNull GameEventSupportManifest supportManifest() {
        return MANIFEST;
    }
}
