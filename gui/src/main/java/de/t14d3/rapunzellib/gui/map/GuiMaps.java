package de.t14d3.rapunzellib.gui.map;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.GuiRendererRegistry;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Entry point for the map renderer.
 * <p>
 * The renderer is registered by name in the {@link GuiRendererRegistry} once
 * the platform has installed the GUI features; {@link #renderer()} returns a
 * proxy that resolves it lazily at render time. This keeps the map API
 * loader-independent: building a map never references a platform module.
 * </p>
 */
public final class GuiMaps {

    /** The name the map renderer is registered under. */
    public static final String RENDERER_NAME = "map";

    private static final Set<GuiCapability> CAPABILITIES = Set.of(
        GuiCapability.MAP_RENDERING,
        GuiCapability.LIVE_TERRAIN,
        GuiCapability.PIXEL_INPUT
    );

    private static final GuiRenderer INSTANCE = new LazyMapRenderer();

    private GuiMaps() {
    }

    /**
     * Returns the map renderer for use with {@code Gui.builder().renderer(...)}
     * or {@link GuiMap#open}.
     *
     * @return the map renderer proxy
     */
    public static @NotNull GuiRenderer renderer() {
        return INSTANCE;
    }

    private static final class LazyMapRenderer implements GuiRenderer {
        @Override
        public @NotNull String name() {
            return RENDERER_NAME;
        }

        @Override
        public @NotNull Set<GuiCapability> capabilities() {
            return CAPABILITIES;
        }

        @Override
        public boolean supports(@NotNull GuiCapability capability) {
            return CAPABILITIES.contains(capability);
        }

        @Override
        public void render(@NotNull de.t14d3.rapunzellib.gui.Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
            resolve().render(gui, player, context);
        }

        @Override
        public void close(@NotNull de.t14d3.rapunzellib.gui.Gui gui, @NotNull RPlayer player) {
            resolve().close(gui, player);
        }

        private @NotNull GuiRenderer resolve() {
            GuiRendererRegistry registry = Rapunzel.findService(GuiRendererRegistry.class)
                .orElseThrow(() -> new IllegalStateException(
                    "GUI renderer registry is not installed; the GUI feature installer must run before the map renderer can be used"));
            return registry.get(RENDERER_NAME);
        }
    }
}
