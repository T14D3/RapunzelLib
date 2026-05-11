package de.t14d3.rapunzellib.events.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InventoryEventPayloadsTest {
    @Test
    void preEventCapturesSlotSnapshotWithoutBreakingInventoryHandleAccess() {
        TestInventory inventory = new TestInventory(new Object(), 3);
        RItem stone = RItem.of("minecraft:stone", 2);
        inventory.setItem(1, stone);

        InventoryClickPre event = InventoryEventPayloads.clickPre(new TestPlayer(), inventory, 1, InventoryClickType.RIGHT);
        inventory.setItem(1, RItem.of("minecraft:diamond", 1));

        assertSame(inventory, event.inventory());
        assertSame(inventory.handle(), event.inventory().handle());
        assertEquals(1, event.slot());
        assertEquals(InventoryClickType.RIGHT, event.clickType());
        assertEquals(stone, event.currentItem().orElseThrow());
        assertFalse(event.isCancelled());
    }

    @Test
    void postEventReflectsFinalCancelledStateAndCurrentSlotSnapshot() {
        TestInventory inventory = new TestInventory(new Object(), 2);
        inventory.setItem(0, RItem.of("minecraft:paper", 1));

        InventoryClickPre pre = InventoryEventPayloads.clickPre(new TestPlayer(), inventory, 0, InventoryClickType.LEFT);
        pre.deny();
        inventory.setItem(0, RItem.of("minecraft:book", 1));

        InventoryClickPost post = InventoryEventPayloads.clickPost(
            pre.player(),
            inventory,
            pre.slot(),
            pre.clickType(),
            pre.isCancelled() || pre.isDenied()
        );

        assertTrue(post.cancelled());
        assertEquals(RItem.of("minecraft:book", 1), post.currentItem().orElseThrow());
        assertSame(inventory, post.inventory());
    }

    @Test
    void openPreEventExposesWrappedInventoryAndPlayerHandleAndSupportsCancellation() {
        TestInventory inventory = new TestInventory(new Object(), 1);
        TestPlayer player = new TestPlayer();

        InventoryOpenPre event = InventoryEventPayloads.openPre(player, inventory);

        event.deny();

        assertTrue(event.isDenied());
        assertFalse(event.isCancelled());
        assertSame(player, event.player());
        assertSame(player.handle(), event.player().handle());
        assertSame(inventory, event.inventory());
        assertSame(inventory.handle(), event.inventory().handle());
    }

    @Test
    void openPostEventExposesWrappedInventoryAndPlayerHandle() {
        TestInventory inventory = new TestInventory(new Object(), 1);
        TestPlayer player = new TestPlayer();

        InventoryOpenPost event = InventoryEventPayloads.openPost(player, inventory);

        assertSame(player, event.player());
        assertSame(player.handle(), event.player().handle());
        assertSame(inventory, event.inventory());
        assertSame(inventory.handle(), event.inventory().handle());
    }

    @Test
    void closeEventExposesWrappedInventoryAndPlayerHandle() {
        TestInventory inventory = new TestInventory(new Object(), 1);
        TestPlayer player = new TestPlayer();

        InventoryClosePost event = InventoryEventPayloads.closePost(player, inventory);

        assertSame(player, event.player());
        assertSame(player.handle(), event.player().handle());
        assertSame(inventory, event.inventory());
        assertSame(inventory.handle(), event.inventory().handle());
    }

    private static final class TestInventory extends RNativeHandle<Object> implements RInventory {
        private final RItem[] contents;

        private TestInventory(@NotNull Object handle, int size) {
            super(PlatformId.PAPER, handle);
            this.contents = new RItem[size];
        }

        @Override
        public int size() {
            return contents.length;
        }

        @Override
        public @NotNull Optional<RItem> item(int slot) {
            return Optional.ofNullable(contents[slot]);
        }

        @Override
        public void setItem(int slot, @Nullable RItem item) {
            contents[slot] = item;
        }
    }

    private static final class TestPlayer extends RNativeHandle<Object> implements RServerPlayer {
        private TestPlayer() {
            super(PlatformId.PAPER, new Object());
        }

        @Override
        public @NotNull Audience audience() {
            return Audience.empty();
        }

        @Override
        public @NotNull UUID uuid() {
            return UUID.fromString("00000000-0000-0000-0000-00000000cafe");
        }

        @Override
        public @NotNull String name() {
            return "player";
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return true;
        }

        @Override
        public @NotNull Optional<RWorld> world() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.empty();
        }

        @Override
        public double health() {
            return 20.0d;
        }

        @Override
        public double maxHealth() {
            return 20.0d;
        }

        @Override
        public int remainingAir() {
            return 300;
        }

        @Override
        public int maxAir() {
            return 300;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public @NotNull Optional<String> getName() {
            return Optional.empty();
        }

        @Override
        public void setName(@NotNull String name) {
        }

        @Override
        public @NotNull Optional<Component> getDisplayName() {
            return Optional.empty();
        }

        @Override
        public void setDisplayName(@NotNull Component displayName) {
        }

        @Override
        public boolean remove() {
            return false;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }
    }
}
