package de.t14d3.rapunzellib.gui.map;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCloseHooks;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.context.CloseReason;
import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.layout.GuiLayout;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for {@link GuiMap} instances.
 * <p>
 * The map renders through the map {@link GuiRenderer}, resolved lazily by name
 * at open time, so building a map never depends on a platform module.
 * </p>
 */
public final class GuiMapBuilder {

    private Component title;
    private int terrainBlocksPerPixel;
    private final List<GuiMapLayer> layers = new ArrayList<>();
    private Consumer<GuiMapClick> onClick;

    GuiMapBuilder() {
    }

    /** Sets the map title, drawn as a header line when the map is rendered as a plain GUI. */
    public @NotNull GuiMapBuilder title(@Nullable Component title) {
        this.title = title;
        return this;
    }

    /**
     * Enables the live terrain background at the given zoom.
     *
     * @param blocksPerPixel the zoom in blocks per pixel, or 0 to disable terrain
     * @return this builder
     */
    public @NotNull GuiMapBuilder terrain(int blocksPerPixel) {
        this.terrainBlocksPerPixel = Math.max(0, blocksPerPixel);
        return this;
    }

    /**
     * Adds a layer, drawn above all previously added layers.
     *
     * @param layer the layer to add
     * @return this builder
     */
    public @NotNull GuiMapBuilder layer(@NotNull GuiMapLayer layer) {
        Objects.requireNonNull(layer, "layer");
        this.layers.add(layer);
        return this;
    }

    /**
     * Sets the click handler.
     *
     * @param onClick the handler, receiving pixel and block coordinates
     * @return this builder
     */
    public @NotNull GuiMapBuilder onClick(@Nullable Consumer<GuiMapClick> onClick) {
        this.onClick = onClick;
        return this;
    }

    /**
     * Builds the map. It is inert until {@link GuiMap#open} is called.
     *
     * @return the map
     */
    public @NotNull GuiMap build() {
        return new BuiltMapGui(title, terrainBlocksPerPixel, List.copyOf(layers), onClick);
    }

    private static final class BuiltMapGui implements GuiMap, GuiCloseHooks.Handler {
        private final GuiRenderer renderer = GuiMaps.renderer();
        private final Component title;
        private final int terrainBlocksPerPixel;
        private final List<GuiMapLayer> layers;
        private final Consumer<GuiMapClick> onClick;
        private final Map<RPlayer, RenderContext> contexts = new HashMap<>();

        private BuiltMapGui(
            @Nullable Component title,
            int terrainBlocksPerPixel,
            @NotNull List<GuiMapLayer> layers,
            @Nullable Consumer<GuiMapClick> onClick
        ) {
            this.title = title;
            this.terrainBlocksPerPixel = terrainBlocksPerPixel;
            this.layers = layers;
            this.onClick = onClick;
        }

        @Override
        public @NotNull GuiRenderer renderer() {
            return renderer;
        }

        @Override
        public @NotNull GuiLayout layout() {
            return () -> Collections.emptyList();
        }

        @Override
        public @Nullable Component title() {
            return title;
        }

        @Override
        public int rows() {
            return 0;
        }

        @Override
        public @NotNull List<GuiMapLayer> layers() {
            return layers;
        }

        @Override
        public int terrainBlocksPerPixel() {
            return terrainBlocksPerPixel;
        }

        @Override
        public @Nullable Consumer<GuiMapClick> onClick() {
            return onClick;
        }

        @Override
        public void open(@NotNull RPlayer player) {
            renderer.render(this, player, getOrCreateContext(player));
        }

        @Override
        public void close(@NotNull RPlayer player) {
            try {
                renderer.close(this, player);
            } finally {
                handleClose(player, CloseReason.PLUGIN);
            }
        }

        @Override
        public void handleClose(@NotNull RPlayer player, @NotNull CloseReason reason) {
            contexts.remove(player);
        }

        private @NotNull RenderContext getOrCreateContext(@NotNull RPlayer player) {
            return contexts.computeIfAbsent(player, p -> new MapRenderContext(p, this));
        }
    }

    private static final class MapRenderContext implements RenderContext {
        private final RPlayer player;
        private final GuiMap gui;
        private final GuiState state = GuiState.create();
        private final Map<Integer, GuiElement> elementRegistry = new HashMap<>();

        private MapRenderContext(@NotNull RPlayer player, @NotNull GuiMap gui) {
            this.player = player;
            this.gui = gui;
        }

        @Override
        public @NotNull RPlayer player() {
            return player;
        }

        @Override
        public @NotNull Gui gui() {
            return gui;
        }

        @Override
        public @NotNull GuiState state() {
            return state;
        }

        @Override
        public @Nullable GuiElement elementAt(int slot) {
            return elementRegistry.get(slot);
        }

        @Override
        public @NotNull Map<Integer, GuiElement> elementRegistry() {
            return Collections.unmodifiableMap(elementRegistry);
        }

        @Override
        public void registerElement(int slot, @NotNull GuiElement element) {
            elementRegistry.put(slot, element);
        }

        @Override
        public void set(@NotNull String key, @NotNull GuiValue value) {
            state.set(key, value);
        }
    }
}
