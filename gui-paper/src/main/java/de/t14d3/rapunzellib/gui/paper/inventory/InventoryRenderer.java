package de.t14d3.rapunzellib.gui.paper.inventory;

import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.gui.core.GuiInventoryElementHandler;
import de.t14d3.rapunzellib.gui.element.ButtonElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.SliderElement;
import de.t14d3.rapunzellib.gui.element.ToggleElement;
import de.t14d3.rapunzellib.gui.shared.inventory.AbstractSharedInventoryRenderer;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper inventory GUI renderer.
 * <p>
 * Delegates the chest-menu, dropdown, and anvil-input rendering and click handling
 * to {@link AbstractSharedInventoryRenderer} (whose menu-slot overrides route through
 * Paperweight-userdev-exposed Mojang mappings). Adds Paper-style click feedback sounds
 * via the {@link #onElementClicked} hook.
 */
public final class InventoryRenderer extends AbstractSharedInventoryRenderer {

    private static final InventoryRenderer INSTANCE = new InventoryRenderer();

    private InventoryRenderer() {
        super("paper-inventory", () -> NbtFeatures.itemStackAdapter(ItemStack.class));
    }

    public static InventoryRenderer instance() {
        return INSTANCE;
    }

    @Nullable
    @Override
    protected ServerPlayer unwrap(@NotNull RPlayer player) {
        return player.tryHandle(ServerPlayer.class).orElse(null);
    }

    @Override
    protected void onElementClicked(
        @NotNull RPlayer player,
        @NotNull GuiElement element,
        @NotNull ClickContext clickContext,
        @NotNull GuiInventoryElementHandler.Result result
    ) {
        ServerPlayer serverPlayer = unwrap(player);
        if (serverPlayer == null) {
            return;
        }
        org.bukkit.entity.Player bukkitPlayer = serverPlayer.getBukkitEntity();

        if (element instanceof ButtonElement button && button.onClick() != null) {
            bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        if (!result.stateMutated()) {
            return;
        }

        switch (element) {
            case ToggleElement toggle -> {
                boolean value = clickContext.state().get(toggle.key(), Boolean.class, toggle.defaultValue());
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, value ? 1.2f : 0.8f);
            }
            case SliderElement slider -> {
                float value = clickContext.state().get(slider.key(), Float.class, slider.defaultValue());
                float maxValue = slider.max() <= 0.0f ? 1.0f : slider.max();
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0f, 1.0f + (value / maxValue));
            }
            default -> { }
        }
    }
}
