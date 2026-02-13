package de.t14d3.rapunzellib.gui.paper.inventory;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InventoryGuiHolder implements InventoryHolder {

    private final Gui gui;
    private final RenderContext context;
    private Inventory inventory;

    public InventoryGuiHolder(@NotNull Gui gui, @NotNull RenderContext context) {
        this.gui = gui;
        this.context = context;
    }

    @NotNull
    public Gui gui() {
        return gui;
    }

    @NotNull
    public RenderContext context() {
        return context;
    }

    void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @Nullable Inventory getInventory() {
        return inventory;
    }
}
