package de.t14d3.rapunzellib.gui.shared.map;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEvents;
import de.t14d3.rapunzellib.gui.GuiRendererRegistry;
import de.t14d3.rapunzellib.gui.map.GuiMaps;
import org.jetbrains.annotations.NotNull;

/**
 * Lazy, idempotent installation of the map feature.
 * <p>
 * The map renderer itself is registered by the per-platform
 * {@code GuiFeatureInstaller} (it is shared NMS code with no platform side, so
 * every installer registers the same {@link SharedMapGuiRenderer} instance).
 * This support class only wires the cross-loader event bus, and registers the
 * renderer as a fallback when a platform installer has not done so yet. A
 * marker service in the context makes the installation run exactly once.
 * </p>
 */
public final class SharedMapSupport {

    private static final SharedMapSupport MARKER = new SharedMapSupport();

    private SharedMapSupport() {
    }

    /**
     * Ensures the map renderer is registered and input is wired.
     * <p>
     * Safe to call on every render; only the first call does any work.
     * </p>
     */
    public static void ensureInstalled() {
        RapunzelContext context = Rapunzel.context();
        FeatureInstallationSupport.install(
            context,
            SharedMapSupport.class,
            MARKER,
            null,
            "GUI map renderer",
            () -> {
                GuiRendererRegistry registry = context.services().find(GuiRendererRegistry.class)
                    .orElseGet(() -> {
                        GuiRendererRegistry created = GuiRendererRegistry.create(context);
                        context.register(GuiRendererRegistry.class, created);
                        return created;
                    });
                if (!registry.has(GuiMaps.RENDERER_NAME)) {
                    registry.registerRenderer(SharedMapGuiRenderer.INSTANCE);
                }
                SharedMapInput.wire(GameEvents.install(context));
            }
        );
    }
}
