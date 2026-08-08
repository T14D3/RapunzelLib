package de.t14d3.rapunzellib.gui.shared.map;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.GuiCapability;
import de.t14d3.rapunzellib.gui.GuiRenderer;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.gui.context.ClickType;
import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.element.ButtonElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.map.GuiMap;
import de.t14d3.rapunzellib.gui.map.GuiMapBuilder;
import de.t14d3.rapunzellib.gui.map.GuiMapCanvas;
import de.t14d3.rapunzellib.gui.map.GuiMapClick;
import de.t14d3.rapunzellib.gui.map.GuiMapColor;
import de.t14d3.rapunzellib.gui.map.GuiMaps;
import de.t14d3.rapunzellib.gui.map.GuiMapRect;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The shared map renderer.
 * <p>
 * Renders a {@link GuiMap} with its full surface (terrain, layers, coordinate
 * clicks). Any other {@link Gui} is flattened: the title and one row per
 * element are drawn as boxes, with button rows dispatching their click
 * handlers. The renderer registers itself lazily under the name
 * {@code "map"} when first used.
 * </p>
 */
public final class SharedMapGuiRenderer implements GuiRenderer {

    public static final SharedMapGuiRenderer INSTANCE = new SharedMapGuiRenderer();

    private static final Set<GuiCapability> CAPABILITIES = Set.of(
        GuiCapability.MAP_RENDERING,
        GuiCapability.LIVE_TERRAIN,
        GuiCapability.PIXEL_INPUT
    );

    private SharedMapGuiRenderer() {
    }

    @Override
    public @NotNull String name() {
        return GuiMaps.RENDERER_NAME;
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
    public void render(@NotNull Gui gui, @NotNull RPlayer player, @NotNull RenderContext context) {
        SharedMapSupport.ensureInstalled();
        if (gui instanceof GuiMap map) {
            SharedMapSession.open(map, player);
        } else {
            SharedMapSession.open(flatten(gui, context), player);
        }
    }

    @Override
    public void close(@NotNull Gui gui, @NotNull RPlayer player) {
        SharedMapSessions.close(player.uuid());
    }

    /**
     * Turns a plain {@link Gui} into a map: the title as a header line, then
     * one row per registered element. Button rows dispatch their click
     * handler; other elements render as boxes.
     */
    private static @NotNull GuiMap flatten(@NotNull Gui gui, @NotNull RenderContext context) {
        List<Map.Entry<Integer, GuiElement>> elements = new ArrayList<>(context.elementRegistry().entrySet());
        elements.sort(Comparator.comparingInt(Map.Entry::getKey));
        String title = gui.title() != null ? PlainTextComponentSerializer.plainText().serialize(gui.title()) : null;

        GuiMapBuilder builder = GuiMap.builder();
        builder.layer((canvas, viewport) -> {
            int y = 2;
            if (title != null && !title.isEmpty()) {
                canvas.text(2, y, title, GuiMapColor.WHITE);
                y += canvas.font().lineHeight() + 3;
            }
            for (Map.Entry<Integer, GuiElement> entry : elements) {
                GuiElement element = entry.getValue();
                GuiMapRect row = new GuiMapRect(2, y, canvas.width() - 4, 10);
                canvas.outlineRect(row, 1, GuiMapColor.WHITE, 2);
                String label = element instanceof ButtonElement button
                    ? PlainTextComponentSerializer.plainText().serialize(button.label())
                    : element.type().name().toLowerCase();
                canvas.text(4, y + 1, label, GuiMapColor.WHITE);
                y += 13;
            }
        });

        builder.onClick(click -> {
            if (click.action() == GuiMapClick.Action.LEFT) {
                return; // buttons activate on the steady hand, like the inventory
            }
            int rowIndex = (click.pixel().y() - 2) / 13 - (title != null && !title.isEmpty() ? 1 : 0);
            if (rowIndex < 0 || rowIndex >= elements.size()) {
                return;
            }
            Map.Entry<Integer, GuiElement> entry = elements.get(rowIndex);
            if (entry.getValue() instanceof ButtonElement button && button.onClick() != null) {
                button.onClick().accept(new FlattenedClickContext(context.player(), button, entry.getKey(), click.action()));
            }
        });
        return builder.build();
    }

    private static final class FlattenedClickContext implements ClickContext {
        private final RPlayer player;
        private final GuiElement element;
        private final int slot;
        private final ClickType clickType;
        private final GuiState state = GuiState.create();

        private FlattenedClickContext(RPlayer player, GuiElement element, int slot, GuiMapClick.Action action) {
            this.player = player;
            this.element = element;
            this.slot = slot;
            this.clickType = action == GuiMapClick.Action.LEFT ? ClickType.LEFT : ClickType.RIGHT;
        }

        @Override
        public @NotNull RPlayer player() {
            return player;
        }

        @Override
        public @NotNull GuiElement element() {
            return element;
        }

        @Override
        public int slot() {
            return slot;
        }

        @Override
        public @NotNull ClickType clickType() {
            return clickType;
        }

        @Override
        public @NotNull GuiState state() {
            return state;
        }
    }
}
