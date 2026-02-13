package de.t14d3.rapunzellib.gui.builder;

import de.t14d3.rapunzellib.gui.*;
import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class DefaultGuiRenderer implements GuiRenderer {
    static final DefaultGuiRenderer INSTANCE = new DefaultGuiRenderer();
    
    private static final Set<GuiCapability> CAPABILITIES = Set.of(
        GuiCapability.GRID_LAYOUT,
        GuiCapability.ITEM_DISPLAY,
        GuiCapability.PAGINATION
    );
    
    @Override
    public @NotNull String name() {
        return "inventory";
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
    }
    
    @Override
    public void close(@NotNull Gui gui, @NotNull RPlayer player) {
    }
}

class DefaultRenderContext implements RenderContext {
    private final RPlayer player;
    private final Gui gui;
    private final GuiState state = GuiState.create();
    private final Map<Integer, GuiElement> elementRegistry = new HashMap<>();
    
    DefaultRenderContext(@NotNull RPlayer player, @NotNull Gui gui) {
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
