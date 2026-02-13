package de.t14d3.rapunzellib.gui.neoforge.inventory;

import de.t14d3.rapunzellib.gui.shared.inventory.AbstractSharedInventoryRenderer;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class InventoryRenderer extends AbstractSharedInventoryRenderer {

    private static final InventoryRenderer INSTANCE = new InventoryRenderer();

    private InventoryRenderer() {
        super("inventory", () -> NbtFeatures.itemStackAdapter(ItemStack.class));
    }

    public static InventoryRenderer instance() {
        return INSTANCE;
    }

    @Nullable
    @Override
    protected ServerPlayer unwrap(RPlayer player) {
        return player.tryHandle(ServerPlayer.class).orElse(null);
    }
}
