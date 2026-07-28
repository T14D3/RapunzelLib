package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.inventory.InventoryClosePost;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerCloseMixin {
    @Inject(method = "removed", at = @At("HEAD"))
    private void onContainerClosePre(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!bus.hasPostListeners(InventoryClosePost.class)) return;

        AbstractContainerMenu container = (AbstractContainerMenu) (Object) this;
        var rInventory = InventoryFeatures.install().wrap(container).orElse(null);
        if (rInventory == null) return;

        bus.dispatchPost(new InventoryClosePost(Rapunzel.players().require(serverPlayer), rInventory));
    }
}
