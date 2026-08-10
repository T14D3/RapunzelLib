package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.inventory.InventoryTransferPre;
import de.t14d3.rapunzellib.events.inventory.InventoryTransferPre.TransferSource;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
// The minecart classes moved into the `vehicle.minecart` subpackage in
// 1.21.11 (verified against the 1.21.11 + 26.1.2/26.2 mapped jars).
// #if VERSION >= 1.21.11
// # import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
// #else
import net.minecraft.world.entity.vehicle.MinecartHopper;
// #endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shared Fabric/NeoForge bridge for {@link InventoryTransferPre}.
 *
 * <p>Injects into the single vanilla choke point through which every
 * inventory-to-inventory carrier transfer flows in 26.1.2 (mojmap names,
 * identical in 1.21.10/1.21.11): {@code HopperBlockEntity.addItem(Container
 * from, Container into, ItemStack stack, Direction direction)}.</p>
 *
 * <p>Call paths covered:</p>
 * <ul>
 *   <li>hopper block push ({@code ejectItems} -> {@code addItem}: {@code from}
 *       = the hopper, {@code into} = the destination container),</li>
 *   <li>hopper block and hopper minecart pull ({@code suckInItems} ->
 *       {@code tryTakeInItemFromSlot} -> {@code addItem}: {@code from} = the
 *       source container, {@code into} = the hopper - minecarts reuse the
 *       hopper's static methods),</li>
 *   <li>dropper push ({@code DropperBlock.dispenseFrom} -> {@code addItem}:
 *       {@code from} = the dropper).</li>
 * </ul>
 *
 * <p>The item-entity pickup path reaches this method with {@code from ==
 * null} and never fires (not an inventory-to-inventory transfer). NeoForge
 * transfers into/out of modded {@code IItemHandler} inventories bypass
 * {@code addItem} (via {@code VanillaInventoryCodeHooks}) and are not
 * covered.</p>
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperTransferMixin {

    @Inject(
        // addItem(Container, Container, ItemStack, Direction) - the
        // overloaded ItemEntity variant must not match, hence the descriptor.
        method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onInventoryTransferPre(
        Container from, Container into, ItemStack stack, Direction direction, CallbackInfoReturnable<ItemStack> cir
    ) {
        // Item-entity pickup: addItem(Container, ItemEntity) delegates with
        // from == null. That is a world -> inventory pickup, not an
        // inventory-to-inventory transfer, and never fires this event.
        if (from == null || stack == null || stack.isEmpty()) return;

        GameEventBus bus = SharedMixinEventsBridge.bus();
        if (bus == null || !bus.hasPreListeners(InventoryTransferPre.class)) return;

        Level level = levelOf(from);
        if (level == null) {
            level = levelOf(into);
        }
        if (!(level instanceof ServerLevel serverLevel)) return;

        TransferSource source = transferSource(from, into);
        RBlockPos sourcePos = blockPosOf(from);
        RBlockPos targetPos = blockPosOf(into);
        // Contract: at least one side is a block (the carrier itself), so at
        // least one position must resolve - except when the carrier is a
        // minecart, in which case the minecart side is null by contract and
        // the other side (the block container) still resolves.
        if (sourcePos == null && targetPos == null) return;

        // #if VERSION >= 1.21.11
        RKey dimKey = RKey.of(serverLevel.dimension().identifier().toString());
        // #else
        RKey dimKey = RKey.of(serverLevel.dimension().location().toString());
        // #endif
        RWorldRef worldRef = new RWorldRef(null, dimKey);
        RItem item = wrapItem(stack);
        if (item == null) return;

        InventoryTransferPre pre = new InventoryTransferPre(
            worldRef,
            sourcePos,
            targetPos,
            item,
            stack.getCount(),
            source
        );
        bus.dispatchPre(pre);
        if (pre.isDenied()) {
            // Returning the input stack unchanged means "nothing moved"; every
            // caller (ejectItems / tryTakeInItemFromSlot / DropperBlock) treats
            // a non-empty return as a failed transfer and restores the item.
            cir.setReturnValue(stack);
        }
    }

    /** Resolves the level of a container via its block-entity or entity backing. */
    private static Level levelOf(Container container) {
        if (container instanceof BlockEntity blockEntity) {
            return blockEntity.getLevel();
        }
        if (container instanceof Entity entity) {
            return entity.level();
        }
        return null;
    }

    /**
     * Resolves the block position of a container, or null when it is not a
     * block (minecart inventories - null by contract) or the position is not
     * exposed (e.g. double chests via {@code CompoundContainer}).
     */
    private static RBlockPos blockPosOf(Container container) {
        if (container instanceof BlockEntity blockEntity && blockEntity.getLevel() != null) {
            BlockPos pos = blockEntity.getBlockPos();
            return new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        }
        return null;
    }

    /**
     * Maps the carrier that initiates the transfer: the hopper block for both
     * eject ({@code from}) and suck ({@code into}) flows, the hopper minecart
     * for minecart pulls ({@code into}), and the dropper block for dropper
     * pushes ({@code from}).
     */
    private static TransferSource transferSource(Container from, Container into) {
        if (from instanceof HopperBlockEntity || into instanceof HopperBlockEntity) {
            return TransferSource.HOPPER;
        }
        if (from instanceof MinecartHopper || into instanceof MinecartHopper) {
            return TransferSource.HOPPER_MINECART;
        }
        if (from instanceof DropperBlockEntity) {
            return TransferSource.DROPPER;
        }
        return TransferSource.HOPPER;
    }

    private static RItem wrapItem(ItemStack stack) {
        try {
            return NbtFeatures.itemStackAdapter(ItemStack.class).snapshot(stack);
        } catch (RuntimeException ignored) {
            // Item-stack adapters may not be installed; the transfer is
            // skipped rather than dispatched with a broken payload.
            return null;
        }
    }
}
