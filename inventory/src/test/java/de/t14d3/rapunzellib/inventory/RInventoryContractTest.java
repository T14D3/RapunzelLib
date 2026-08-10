package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.item.RItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RInventoryContractTest {
    @Test
    void liveWrapperNormalizesEmptySlotsAndTracksNativeMutations() {
        TestSupport.TestNativeInventory nativeInventory = new TestSupport.TestNativeInventory(3);
        Inventories inventories = new DefaultInventories(PlatformId.PAPER, List.of(TestSupport.testFactory(PlatformId.PAPER)));

        RInventory inventory = inventories.require(nativeInventory);

        assertSame(nativeInventory, inventory.handle());
        assertEquals(3, inventory.size());
        assertTrue(inventory.item(0).isEmpty());

        inventory.setItem(0, RItem.of("minecraft:stone", 2));
        assertEquals(RKey.of("minecraft:stone"), inventory.item(0).orElseThrow().typeKey());

        nativeInventory.set(1, new TestSupport.TestNativeItem("minecraft:bread", 1));
        assertEquals(RKey.of("minecraft:bread"), inventory.item(1).orElseThrow().typeKey());

        inventory.setItem(1, null);
        inventory.setItem(2, RItem.of("minecraft:air", 1));

        List<Optional<RItem>> contents = inventory.contents();
        assertEquals(3, contents.size());
        assertEquals(RKey.of("minecraft:stone"), contents.get(0).orElseThrow().typeKey());
        assertTrue(contents.get(1).isEmpty());
        assertTrue(contents.get(2).isEmpty());

        inventory.clear();
        assertTrue(inventory.contents().stream().allMatch(Optional::isEmpty));
    }

    @Test
    void rejectsOutOfBoundsSlotAccess() {
        RInventory inventory = new DefaultInventories(PlatformId.PAPER, List.of(TestSupport.testFactory(PlatformId.PAPER)))
            .require(new TestSupport.TestNativeInventory(1));

        assertThrows(IndexOutOfBoundsException.class, () -> inventory.item(1));
        assertThrows(IndexOutOfBoundsException.class, () -> inventory.setItem(-1, RItem.of("minecraft:stone")));
    }

    @Test
    void playerInventoryStartFallsBackToSizeMinus36Invariant() {
        // A plain wrap without a platform-specific computation uses the
        // documented full-menu invariant: player section = last 36 slots.
        RInventory inventory = new DefaultInventories(PlatformId.PAPER, List.of(TestSupport.testFactory(PlatformId.PAPER)))
            .require(new TestSupport.TestNativeInventory(63)); // 27 top + 36 player

        assertEquals(27, inventory.playerInventoryStart());
    }

    @Test
    void playerInventoryStartUsesPlatformComputationWhenProvided() {
        // An adapter with an explicit computation wins over the invariant -
        // e.g. the player's own CRAFTING view, where the player section
        // (armor + storage + hotbar + offhand) starts at the top size.
        InventoryWrapperFactory<TestSupport.TestNativeInventory> factory =
            InventoryFeatureInstallerSupport.slotInventoryFactory(
                PlatformId.PAPER,
                InventoryFeatureInstallerSupport.SlotInventoryAdapter
                    .<TestSupport.TestNativeInventory, TestSupport.TestNativeItem>builder(
                        TestSupport.TestNativeInventory.class,
                        new TestSupport.TestItemStackAdapter()
                    )
                    .size(TestSupport.TestNativeInventory::size)
                    .playerInventoryStart(inv -> 5) // exact top size of a CRAFTING view
                    .getItem(TestSupport.TestNativeInventory::item)
                    .setItem(TestSupport.TestNativeInventory::set)
                    .isEmptyItem(TestSupport.TestNativeItem::isEmpty)
                    .emptyItem(TestSupport.TestNativeItem::empty)
                    .build()
            );

        RInventory inventory = new DefaultInventories(PlatformId.PAPER, List.of(factory))
            .require(new TestSupport.TestNativeInventory(46));

        assertEquals(5, inventory.playerInventoryStart());
    }
}
