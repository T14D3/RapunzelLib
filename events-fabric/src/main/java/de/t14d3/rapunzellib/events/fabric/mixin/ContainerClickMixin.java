package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPost;
import de.t14d3.rapunzellib.events.inventory.InventoryClickPre;
import de.t14d3.rapunzellib.events.inventory.InventoryClickType;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
// #if VERSION >= 26
// # import net.minecraft.world.inventory.ContainerInput;
// #else
import net.minecraft.world.inventory.ClickType;
// #endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (!bus.hasPreListeners(InventoryClickPre.class)) return;

        var rPlayer = Rapunzel.players().require(serverPlayer);
        AbstractContainerMenu container = (AbstractContainerMenu) (Object) this;
        var rInventory = InventoryFeatures.install().wrap(container).orElse(null);
        if (rInventory == null) return;

        InventoryClickType clickTypeMapped = switch (clickType) {
            case PICKUP -> InventoryClickType.LEFT;
            case QUICK_MOVE -> InventoryClickType.SHIFT_LEFT;
            case SWAP -> InventoryClickType.SWAP_OFFHAND;
            case CLONE -> InventoryClickType.MIDDLE;
            case THROW -> InventoryClickType.DROP;
            case QUICK_CRAFT -> InventoryClickType.UNKNOWN;
            case PICKUP_ALL -> InventoryClickType.DOUBLE_CLICK;
        };

        InventoryClickPre pre = new InventoryClickPre(rPlayer, rInventory, slotId, clickTypeMapped);
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
        if (!bus.hasPostListeners(InventoryClickPost.class)) return;

        var rPlayer = Rapunzel.players().require(serverPlayer);
        AbstractContainerMenu container = (AbstractContainerMenu) (Object) this;
        var rInventory = InventoryFeatures.install().wrap(container).orElse(null);
        if (rInventory == null) return;

        InventoryClickType clickTypeMapped = switch (clickType) {
            case PICKUP -> InventoryClickType.LEFT;
            case QUICK_MOVE -> InventoryClickType.SHIFT_LEFT;
            case SWAP -> InventoryClickType.SWAP_OFFHAND;
            case CLONE -> InventoryClickType.MIDDLE;
            case THROW -> InventoryClickType.DROP;
            case QUICK_CRAFT -> InventoryClickType.UNKNOWN;
            case PICKUP_ALL -> InventoryClickType.DOUBLE_CLICK;
        };

        boolean cancelled = !Boolean.TRUE.equals(cir.getReturnValue());
        bus.dispatchPost(new InventoryClickPost(rPlayer, rInventory, slotId, clickTypeMapped, cancelled));
    }
}