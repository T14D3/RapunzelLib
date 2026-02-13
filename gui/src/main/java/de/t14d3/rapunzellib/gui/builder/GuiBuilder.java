package de.t14d3.rapunzellib.gui.builder;

import de.t14d3.rapunzellib.gui.*;
import de.t14d3.rapunzellib.gui.animation.Animation;
import de.t14d3.rapunzellib.gui.context.CloseContext;
import de.t14d3.rapunzellib.gui.context.CloseReason;
import de.t14d3.rapunzellib.gui.element.*;
import de.t14d3.rapunzellib.gui.layout.GridLayout;
import de.t14d3.rapunzellib.gui.layout.GuiLayout;
import de.t14d3.rapunzellib.gui.layout.LinearLayout;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GuiBuilder {
    private Component title;
    private GuiRenderer renderer;
    private int rows = 6;
    private final List<GuiElement> linearElements = new ArrayList<>();
    private final Map<Integer, GuiElement> slots = new HashMap<>();
    private GuiElement fillElement;
    private Consumer<CloseContext> onClose;
    private final List<Animation> animations = new ArrayList<>();
    private boolean useGridLayout = true;
    private boolean linearVertical = true;
    
    @NotNull
    public GuiBuilder title(@NotNull Component title) {
        this.title = title;
        return this;
    }
    
    @NotNull
    public GuiBuilder title(@NotNull String title) {
        return title(Component.text(title));
    }
    
    @NotNull
    public GuiBuilder renderer(@Nullable GuiRenderer renderer) {
        this.renderer = renderer;
        return this;
    }
    
    @NotNull
    public GuiBuilder rows(int rows) {
        requireRows(rows);
        this.rows = rows;
        this.useGridLayout = true;
        return this;
    }
    
    @NotNull
    public GuiBuilder element(@NotNull GuiElement element) {
        if (useGridLayout) {
            int nextSlot = findNextAvailableSlot();
            slots.put(nextSlot, element);
        } else {
            linearElements.add(element);
        }
        return this;
    }
    
    private int findNextAvailableSlot() {
        int capacity = rows * 9;
        for (int i = 0; i < capacity; i++) {
            if (!slots.containsKey(i)) {
                return i;
            }
        }
        throw new IllegalStateException("Grid layout is full (capacity=" + capacity + ")");
    }
    
    @NotNull
    public GuiBuilder button(@NotNull Consumer<ButtonBuilder> config) {
        ButtonBuilder builder = new ButtonBuilder();
        config.accept(builder);
        return element(builder.build());
    }
    
    @NotNull
    public GuiBuilder text(@NotNull Consumer<TextBuilder> config) {
        TextBuilder builder = new TextBuilder();
        config.accept(builder);
        return element(builder.build());
    }
    
    @NotNull
    public GuiBuilder input(@NotNull Consumer<InputBuilder> config) {
        InputBuilder builder = new InputBuilder();
        config.accept(builder);
        return element(builder.build());
    }
    
    @NotNull
    public GuiBuilder toggle(@NotNull Consumer<ToggleBuilder> config) {
        ToggleBuilder builder = new ToggleBuilder();
        config.accept(builder);
        return element(builder.build());
    }
    
    @NotNull
    public GuiBuilder slider(@NotNull Consumer<SliderBuilder> config) {
        SliderBuilder builder = new SliderBuilder();
        config.accept(builder);
        return element(builder.build());
    }
    
    @NotNull
    public GuiBuilder dropdown(@NotNull Consumer<DropdownBuilder> config) {
        DropdownBuilder builder = new DropdownBuilder();
        config.accept(builder);
        return element(builder.build());
    }
    
    @NotNull
    public GuiBuilder item(@NotNull Consumer<ItemBuilder> config) {
        ItemBuilder builder = new ItemBuilder();
        config.accept(builder);
        return element(builder.build());
    }
    
    @NotNull
    public GuiBuilder divider() {
        return element(DividerElement.horizontal());
    }
    
    @NotNull
    public GuiBuilder divider(boolean vertical) {
        return element(vertical ? DividerElement.vertical() : DividerElement.horizontal());
    }
    
    @NotNull
    public GuiBuilder spacer(int width, int height) {
        return element(SpacerElement.of(width, height));
    }
    
    @NotNull
    public GuiBuilder pagination(@NotNull Consumer<PaginationBuilder> config) {
        PaginationBuilder builder = new PaginationBuilder();
        config.accept(builder);
        return element(builder.build());
    }
    
    @NotNull
    public GuiBuilder slot(int slot, @NotNull GuiElement element) {
        this.useGridLayout = true;
        requireGridSlot(slot, rows);
        this.slots.put(slot, element);
        return this;
    }
    
    @NotNull
    public GuiBuilder row(int row, @NotNull GuiElement... elements) {
        this.useGridLayout = true;
        if (row < 0 || row >= rows) {
            throw new IllegalArgumentException("row must be between 0 and " + (rows - 1) + " but was " + row);
        }
        int startSlot = row * 9;
        for (int i = 0; i < elements.length && i < 9; i++) {
            slots.put(startSlot + i, elements[i]);
        }
        return this;
    }
    
    @NotNull
    public GuiBuilder fill(@NotNull GuiElement element) {
        this.fillElement = element;
        return this;
    }
    
    @NotNull
    public GuiBuilder onClose(@Nullable Consumer<CloseContext> handler) {
        this.onClose = handler;
        return this;
    }
    
    @NotNull
    public GuiBuilder animate(@NotNull Animation animation) {
        this.animations.add(animation);
        return this;
    }
    
    @NotNull
    public GuiBuilder linear() {
        return linear(true);
    }

    @NotNull
    public GuiBuilder linear(boolean vertical) {
        this.useGridLayout = false;
        this.linearVertical = vertical;
        return this;
    }
    
    @NotNull
    public Gui build() {
        if (title == null) {
            throw new IllegalStateException("Title is required");
        }

        final boolean finalUseGridLayout = useGridLayout;
        final int finalRows = finalUseGridLayout ? rows : 0;
        final boolean finalLinearVertical = linearVertical;
        final GuiRenderer finalRenderer = renderer;
        final Component finalTitle = title;
        final Consumer<CloseContext> finalOnClose = onClose;

        final GuiLayout layout;
        if (finalUseGridLayout) {
            GridLayout.Builder layoutBuilder = GridLayout.builder().rows(finalRows);
            slots.forEach(layoutBuilder::slot);
            if (fillElement != null) {
                layoutBuilder.fill(fillElement);
            }
            layout = layoutBuilder.build();
        } else {
            LinearLayout.Builder layoutBuilder = LinearLayout.builder().vertical(finalLinearVertical);
            linearElements.forEach(layoutBuilder::element);
            layout = layoutBuilder.build();
        }

        return new BuiltGui(finalRenderer, layout, finalTitle, finalRows, finalOnClose);
    }

    private static void requireRows(int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("rows must be between 1 and 6 but was " + rows);
        }
    }

    private static void requireGridSlot(int slot, int rows) {
        int maxSlot = rows * 9;
        if (slot < 0 || slot >= maxSlot) {
            throw new IllegalArgumentException("slot must be between 0 and " + (maxSlot - 1) + " but was " + slot);
        }
    }

    private static final class BuiltGui implements Gui, GuiCloseHooks.Handler {
        private final GuiRenderer renderer;
        private final GuiLayout layout;
        private final Component title;
        private final int rows;
        private final Consumer<CloseContext> onClose;
        private final Map<RPlayer, RenderContext> contexts = new HashMap<>();

        private BuiltGui(
            @Nullable GuiRenderer renderer,
            @NotNull GuiLayout layout,
            @Nullable Component title,
            int rows,
            @Nullable Consumer<CloseContext> onClose
        ) {
            this.renderer = renderer != null ? renderer : DefaultGuiRenderer.INSTANCE;
            this.layout = layout;
            this.title = title;
            this.rows = rows;
            this.onClose = onClose;
        }

        @Override
        public @NotNull GuiRenderer renderer() {
            return renderer;
        }

        @Override
        public @NotNull GuiLayout layout() {
            return layout;
        }

        @Override
        public @Nullable Component title() {
            return title;
        }

        @Override
        public int rows() {
            return rows;
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
            RenderContext context = contexts.remove(player);
            if (context == null || onClose == null) {
                return;
            }
            onClose.accept(new DefaultCloseContext(player, this, reason));
        }

        private RenderContext getOrCreateContext(@NotNull RPlayer player) {
            return contexts.computeIfAbsent(player, current -> new DefaultRenderContext(current, this));
        }
    }

    private record DefaultCloseContext(
        @NotNull RPlayer player,
        @NotNull Gui gui,
        @NotNull CloseReason reason
    ) implements CloseContext {
    }
}
