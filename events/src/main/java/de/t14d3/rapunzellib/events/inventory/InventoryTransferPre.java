package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Pre-event fired before an item is transferred between two inventories by an
 * automated carrier (hopper block, hopper minecart or dropper).
 *
 * <p>This event is cancellable. If denied, the item transfer does not happen:
 * the item stays in the inventory it was leaving (the {@link #sourcePos()}
 * inventory).</p>
 *
 * <p><b>Position contract (unified across all platforms):</b>
 * {@link #sourcePos()} is the position of the inventory the item moves OUT of
 * - the protected surface for extraction gates - and {@link #targetPos()} is
 * the position of the destination inventory. A position is {@code null} when
 * the corresponding inventory is not a block (e.g. a hopper minecart) or its
 * position cannot be resolved; at least one of the two is always non-null for
 * an inventory-to-inventory transfer.</p>
 *
 * <p>{@link #source()} identifies the carrier that initiates the transfer
 * (the hopper block / hopper minecart / dropper). {@link #item()} is the item
 * being moved and {@link #amount()} the number of items carried by this
 * transfer operation.</p>
 *
 * <p>Platform coverage: Paper fires from
 * {@code org.bukkit.event.inventory.InventoryMoveItemEvent} (both directions,
 * hopper/dropper/minecart); Fabric and NeoForge fire from a shared mixin on
 * {@code HopperBlockEntity.addItem}. Sponge's
 * {@code TransferInventoryEvent.Pre} exposes no item/amount information, so
 * the event is unsupported there.</p>
 */
public final class InventoryTransferPre extends BaseCancellablePreEvent {

    /**
     * The carrier that initiates the transfer.
     */
    public enum TransferSource {
        /** A hopper block (pushing out or pulling in). */
        HOPPER,
        /** A hopper minecart (pulling from the container below/above it). */
        HOPPER_MINECART,
        /** A dropper block pushing into an adjacent container. */
        DROPPER
    }

    private final RWorldRef world;
    private final RBlockPos sourcePos;
    private final RBlockPos targetPos;
    private final RItem item;
    private final int amount;
    private final TransferSource source;

    public InventoryTransferPre(
        @NotNull RWorldRef world,
        @Nullable RBlockPos sourcePos,
        @Nullable RBlockPos targetPos,
        @NotNull RItem item,
        int amount,
        @NotNull TransferSource source
    ) {
        this(world, sourcePos, targetPos, item, amount, source, false);
    }

    public InventoryTransferPre(
        @NotNull RWorldRef world,
        @Nullable RBlockPos sourcePos,
        @Nullable RBlockPos targetPos,
        @NotNull RItem item,
        int amount,
        @NotNull TransferSource source,
        boolean cancelled
    ) {
        this.world = Objects.requireNonNull(world, "world");
        if (sourcePos == null && targetPos == null) {
            throw new IllegalArgumentException("at least one of sourcePos/targetPos must be non-null");
        }
        this.sourcePos = sourcePos;
        this.targetPos = targetPos;
        this.item = Objects.requireNonNull(item, "item");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.amount = amount;
        this.source = Objects.requireNonNull(source, "source");
        setCancelled(cancelled);
    }

    /** Returns the world the transfer happens in. */
    public @NotNull RWorldRef world() {
        return world;
    }

    /**
     * Returns the position of the inventory the item moves OUT of, or
     * {@code null} when that inventory is not a block (e.g. a hopper
     * minecart) or its position cannot be resolved.
     *
     * @return the extraction-side position, or null
     */
    public @Nullable RBlockPos sourcePos() {
        return sourcePos;
    }

    /**
     * Returns the position of the destination inventory, or {@code null}
     * when it is not a block or its position cannot be resolved.
     *
     * @return the destination position, or null
     */
    public @Nullable RBlockPos targetPos() {
        return targetPos;
    }

    /**
     * Returns the item being transferred.
     *
     * @return the moved item
     */
    public @NotNull RItem item() {
        return item;
    }

    /**
     * Returns the number of items moved by this transfer operation.
     *
     * @return the transferred amount, always positive
     */
    public int amount() {
        return amount;
    }

    /**
     * Returns the carrier that initiates the transfer.
     *
     * @return the transfer source
     */
    public @NotNull TransferSource source() {
        return source;
    }
}
