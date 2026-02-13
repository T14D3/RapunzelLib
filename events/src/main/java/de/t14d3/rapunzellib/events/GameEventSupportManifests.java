package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GameEventSupportManifests {
    public static final String GUI_INVENTORY_BRIDGE_DETAILS = "Rapunzel GUI inventory integration bridge";
    public static final String GUI_INVENTORY_BRIDGE_PARTIAL_DETAILS =
        "Rapunzel GUI inventory integration bridge (inventory renderers only; dialog renderers bypass inventory events)";

    private GameEventSupportManifests() {
    }

    public static @NotNull GameEventSupportManifest.Builder withGuiInventoryBridgeSupport(
        @NotNull GameEventSupportManifest.Builder builder
    ) {
        Objects.requireNonNull(builder, "builder");
        return builder.emulatedSupport(
            GUI_INVENTORY_BRIDGE_DETAILS,
            InventoryClickPre.class,
            InventoryClickPost.class,
            InventoryOpenPre.class,
            InventoryOpenPost.class,
            InventoryClosePost.class
        );
    }

    public static @NotNull GameEventSupportManifest guiInventoryBridgeSupport(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return withGuiInventoryBridgeSupport(GameEventSupportManifest.builder(platformId)).build();
    }

    public static @NotNull GameEventSupportManifest.Builder withPartialGuiInventoryBridgeSupport(
        @NotNull GameEventSupportManifest.Builder builder
    ) {
        Objects.requireNonNull(builder, "builder");
        return builder.partialSupport(
            GUI_INVENTORY_BRIDGE_PARTIAL_DETAILS,
            InventoryClickPre.class,
            InventoryClickPost.class,
            InventoryOpenPre.class,
            InventoryOpenPost.class,
            InventoryClosePost.class
        );
    }

    public static @NotNull GameEventSupportManifest partialGuiInventoryBridgeSupport(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return withPartialGuiInventoryBridgeSupport(GameEventSupportManifest.builder(platformId)).build();
    }

}
