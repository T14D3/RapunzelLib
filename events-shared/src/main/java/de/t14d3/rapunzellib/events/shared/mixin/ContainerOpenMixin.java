package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPre;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ContainerOpenMixin {
    @Inject(method = "openMenu", at = @At("RETURN"))
    private void onOpenMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (bus.hasPreListeners(InventoryOpenPre.class) || bus.hasPostListeners(InventoryOpenPost.class)) {
            AbstractContainerMenu container = self.containerMenu;
            if (container == null) return;

            var rPlayer = Rapunzel.players().require(self);
            var rInventory = InventoryFeatures.install().wrap(container).orElse(null);
            if (rInventory == null) return;

            // Pre is advisory on this bridge: the menu has already been opened by the
            // time openMenu returns (mirrors NeoForge's PlayerContainerEvent.Open
            // semantics, where the event is not cancellable). Post still fires.
            if (bus.hasPreListeners(InventoryOpenPre.class)) {
                bus.dispatchPre(new InventoryOpenPre(rPlayer, rInventory));
            }
            if (bus.hasPostListeners(InventoryOpenPost.class)) {
                bus.dispatchPost(new InventoryOpenPost(rPlayer, rInventory));
            }
        }
    }
}
