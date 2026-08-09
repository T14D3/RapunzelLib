package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPost;
import de.t14d3.rapunzellib.events.inventory.InventoryActionPre;
import de.t14d3.rapunzellib.events.inventory.InventoryActionType;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.nbt.item.RItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
// #if VERSION >= 26
// # import net.minecraft.world.inventory.ContainerInput;
// #else
import net.minecraft.world.inventory.ClickType;
// #endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerClickMixin {
    // #if VERSION >= 26
    // # @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    // # private void onContainerClickPre(int slotId, int button, ContainerInput clickType, Player player, CallbackInfoReturnable<Boolean> cir) {
    // #else
    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void onContainerClickPre(int slotId, int button, ClickType clickType, Player player, CallbackInfoReturnable<Boolean> cir) {
    // #endif
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!bus.hasPreListeners(InventoryActionPre.class)) return;

        var rPlayer = Rapunzel.players().require(serverPlayer);
        AbstractContainerMenu container = (AbstractContainerMenu) (Object) this;
        var rInventory = InventoryFeatures.install().wrap(container).orElse(null);
        if (rInventory == null) return;

        // Faithful to the Bukkit ClickType set: the vanilla button index
        // distinguishes left/right variants, and SWAP targets the hotbar
        // (button 0-8, NUMBER_KEY) or the offhand (button 40, SWAP_OFFHAND).
        InventoryActionType actionTypeMapped = switch (clickType) {
            case PICKUP -> button == 0 ? InventoryActionType.LEFT : InventoryActionType.RIGHT;
            case QUICK_MOVE -> button == 0 ? InventoryActionType.SHIFT_LEFT : InventoryActionType.SHIFT_RIGHT;
            case SWAP -> button == 40 ? InventoryActionType.SWAP_OFFHAND : InventoryActionType.NUMBER_KEY;
            case CLONE -> InventoryActionType.MIDDLE;
            case THROW -> button == 0 ? InventoryActionType.DROP : InventoryActionType.CONTROL_DROP;
            case QUICK_CRAFT -> InventoryActionType.DRAG;
            case PICKUP_ALL -> InventoryActionType.DOUBLE_CLICK;
        };

        InventoryActionPre pre = new InventoryActionPre(
            rPlayer,
            rInventory,
            List.of(slotId),
            actionTypeMapped,
            wrapItem(container.getCarried()),
            wrapSlotItem(container, slotId)
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            cir.setReturnValue(false);
        }
    }

    // #if VERSION >= 26
    // # @Inject(method = "doClick", at = @At("RETURN"))
    // # private void onMouseClickPost(int slotId, int button, ContainerInput clickType, Player player, CallbackInfoReturnable<Boolean> cir) {
    // #else
    @Inject(method = "doClick", at = @At("RETURN"))
    private void onContainerClickPost(int slotId, int button, ClickType clickType, Player player, CallbackInfoReturnable<Boolean> cir) {
    // #endif
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!bus.hasPostListeners(InventoryActionPost.class)) return;

        var rPlayer = Rapunzel.players().require(serverPlayer);
        AbstractContainerMenu container = (AbstractContainerMenu) (Object) this;
        var rInventory = InventoryFeatures.install().wrap(container).orElse(null);
        if (rInventory == null) return;

        // Faithful to the Bukkit ClickType set: the vanilla button index
        // distinguishes left/right variants, and SWAP targets the hotbar
        // (button 0-8, NUMBER_KEY) or the offhand (button 40, SWAP_OFFHAND).
        InventoryActionType actionTypeMapped = switch (clickType) {
            case PICKUP -> button == 0 ? InventoryActionType.LEFT : InventoryActionType.RIGHT;
            case QUICK_MOVE -> button == 0 ? InventoryActionType.SHIFT_LEFT : InventoryActionType.SHIFT_RIGHT;
            case SWAP -> button == 40 ? InventoryActionType.SWAP_OFFHAND : InventoryActionType.NUMBER_KEY;
            case CLONE -> InventoryActionType.MIDDLE;
            case THROW -> button == 0 ? InventoryActionType.DROP : InventoryActionType.CONTROL_DROP;
            case QUICK_CRAFT -> InventoryActionType.DRAG;
            case PICKUP_ALL -> InventoryActionType.DOUBLE_CLICK;
        };

        boolean cancelled = !Boolean.TRUE.equals(cir.getReturnValue());
        bus.dispatchPost(new InventoryActionPost(
            rPlayer,
            rInventory,
            List.of(slotId),
            actionTypeMapped,
            wrapItem(container.getCarried()),
            wrapSlotItem(container, slotId),
            cancelled
        ));
    }

    /**
     * Resolves the item in the clicked menu slot, mirroring the Bukkit path's
     * {@code currentItem}: the raw slot indexes the full menu slot list (top
     * container + player inventory section), so the player's own CRAFTING-view
     * main/hotbar slots (9-44) resolve instead of falling out of the top-only
     * wrap bounds.
     *
     * @param container the clicked menu
     * @param slotId    the raw slot index
     * @return the item, or null when the slot is out of bounds or empty
     */
    private static RItem wrapSlotItem(AbstractContainerMenu container, int slotId) {
        if (slotId < 0 || slotId >= container.slots.size()) {
            return null;
        }
        return wrapItem(container.getSlot(slotId).getItem());
    }

    /**
     * Converts the menu's carried item (Bukkit {@code getCursor()} equivalent)
     * to the payload's {@code cursorItem}.
     *
     * @param stack the carried item, may be empty
     * @return the item, or null when empty
     */
    private static RItem wrapItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return NbtFeatures.itemStackAdapter(ItemStack.class).snapshot(stack);
    }
}