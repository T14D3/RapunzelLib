package de.t14d3.rapunzellib.events.fabric.mixin;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.inventory.InventoryOpenPost;
import de.t14d3.rapunzellib.events.shared.mixin.SharedMixinEventsBridge;
import de.t14d3.rapunzellib.inventory.InventoryFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ContainerOpenMixin {
    @Inject(method = "openMenu", at = @At("RETURN"))
    private void onOpenMenuPost(MenuProvider provider, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null) return;
        if (!bus.hasPostListeners(InventoryOpenPost.class)) return;

        AbstractContainerMenu container = self.containerMenu;
        if (container == null) return;

        var rInventory = InventoryFeatures.install().wrap(container).orElse(null);
        if (rInventory == null) return;

        bus.dispatchPost(new InventoryOpenPost(Rapunzel.players().require(self), rInventory));
    }
}
