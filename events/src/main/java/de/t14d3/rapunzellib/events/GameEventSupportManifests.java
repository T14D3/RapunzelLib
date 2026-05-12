package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Helper methods for creating {@link GameEventSupportManifest}s with
 * GUI inventory bridge support.
 *
 * <p>Provides pre-configured builder methods for platforms that integrate
 * with the Rapunzel GUI inventory system.</p>
 */
public final class GameEventSupportManifests {
    public static final String GUI_INVENTORY_BRIDGE_DETAILS = "Rapunzel GUI inventory integration bridge";
    public static final String GUI_INVENTORY_BRIDGE_PARTIAL_DETAILS =
        "Rapunzel GUI inventory integration bridge (inventory renderers only; dialog renderers bypass inventory events)";

    private GameEventSupportManifests() {
    }

    /**
     * Adds emulated inventory bridge support to the given builder.
     *
     * @param builder the builder to augment
     * @return the builder with inventory support added
     */
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

    /**
     * Creates a manifest with emulated GUI inventory bridge support for the given platform.
     *
     * @param platformId the platform identifier
     * @return the support manifest
     */
    public static @NotNull GameEventSupportManifest guiInventoryBridgeSupport(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return withGuiInventoryBridgeSupport(GameEventSupportManifest.builder(platformId)).build();
    }

    /**
     * Adds partial inventory bridge support to the given builder.
     *
     * @param builder the builder to augment
     * @return the builder with partial inventory support added
     */
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

    /**
     * Creates a manifest with partial GUI inventory bridge support for the given platform.
     *
     * @param platformId the platform identifier
     * @return the support manifest
     */
    public static @NotNull GameEventSupportManifest partialGuiInventoryBridgeSupport(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return withPartialGuiInventoryBridgeSupport(GameEventSupportManifest.builder(platformId)).build();
    }

}
