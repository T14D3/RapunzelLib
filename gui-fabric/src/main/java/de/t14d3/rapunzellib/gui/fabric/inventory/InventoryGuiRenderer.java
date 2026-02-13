package de.t14d3.rapunzellib.gui.fabric.inventory;

import de.t14d3.rapunzellib.gui.shared.inventory.AbstractSharedInventoryRenderer;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class InventoryGuiRenderer extends AbstractSharedInventoryRenderer {

    public static final InventoryGuiRenderer INSTANCE = new InventoryGuiRenderer();

    private InventoryGuiRenderer() {
        super("fabric-inventory", () -> NbtFeatures.itemStackAdapter(ItemStack.class));
    }

    @Nullable
    @Override
    protected ServerPlayer unwrap(RPlayer player) {
        return player.tryHandle(ServerPlayer.class).orElse(null);
    }
}
