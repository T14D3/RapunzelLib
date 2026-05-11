package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.registry.RItemType;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeRItemTest {
    @Test
    void nativeBackedItemsExposeHandleAndUpdateThroughSharedState() {
        RItem snapshot = RItem.builder()
            .material("minecraft:stone")
            .amount(2)
            .name(Component.text("Stone"))
            .build();

        String[] capturedHandle = new String[1];
        RItem[] capturedMutation = new RItem[1];

        NativeRItem<String> item = NativeRItem.of(
            PlatformId.FABRIC,
            "native:stone:2",
            handle -> snapshot,
            (handle, mutation) -> {
                capturedHandle[0] = handle;
                capturedMutation[0] = mutation;
            }
        );

        assertEquals(PlatformId.FABRIC, item.platformId());
        assertEquals("native:stone:2", item.handle());
        assertEquals(RItemType.ref("minecraft:stone"), item.typeRef());
        assertEquals(2, item.amount());

        RItem updated = item.withAmount(4);
        assertSame(item, updated);

        assertEquals("native:stone:2", capturedHandle[0]);
        assertEquals(RKey.of("minecraft:stone"), capturedMutation[0].typeKey());
        assertEquals(4, capturedMutation[0].amount());
    }

    @Test
    void simpleAndNativeItemsCompareBySharedState() {
        RNbtCompound sharedData = RNbtCompound.empty()
            .put("payload", RNbtValue.byteArray(new byte[] {1, 2, 3}));

        RItem simple = RItem.builder()
            .material("minecraft:paper")
            .amount(1)
            .custom("payload", RNbtValue.byteArray(new byte[] {1, 2, 3}))
            .build();

        NativeRItem<String> nativeItem = NativeRItem.of(
            PlatformId.PAPER,
            "paper-handle",
            handle -> RItem.builder()
                .material("minecraft:paper")
                .amount(1)
                .custom("payload", RNbtValue.byteArray(new byte[] {1, 2, 3}))
                .build(),
            (currentHandle, _updatedItem) -> {
            }
        );

        assertEquals(simple, nativeItem);
        assertEquals(simple.hashCode(), nativeItem.hashCode());
    }

    @Test
    void customModelDataCanBeClearedWithoutDroppingNativeBacking() {
        RItem[] capturedMutation = new RItem[1];

        NativeRItem<String> item = NativeRItem.of(
            PlatformId.NEOFORGE,
            "cmd:7",
            handle -> RItem.builder()
                .material("minecraft:paper")
                .customModelData(7)
                .build(),
            (_currentHandle, updatedItem) -> {
                capturedMutation[0] = updatedItem;
            }
        );

        RItem updated = item.withoutCustomModelData();
        assertSame(item, updated);

        assertTrue(capturedMutation[0].customModelData().isEmpty());
    }
}
