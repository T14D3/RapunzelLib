package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.registry.RItemType;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeRItemTest {
    @Test
    void nativeBackedItemsExposeHandleAndUpdateThroughSharedState() {
        RItem snapshot = RItem.builder()
            .material("minecraft:stone")
            .amount(2)
            .name(Component.text("Stone"))
            .build();

        NativeRItem<String> item = NativeRItem.of(
            PlatformId.FABRIC,
            "native:stone:2",
            snapshot,
            (_currentHandle, updatedItem) -> updatedItem.typeKey().asString() + ":" + updatedItem.amount()
        );

        assertEquals(PlatformId.FABRIC, item.platformId());
        assertEquals("native:stone:2", item.handle());
        assertEquals(RItemType.ref("minecraft:stone"), item.typeRef());

        RItem updated = item.withAmount(4);
        NativeRItem<?> updatedNative = assertInstanceOf(NativeRItem.class, updated);

        assertEquals(RKey.of("minecraft:stone"), updatedNative.typeKey());
        assertEquals(RItemType.ref("minecraft:stone"), updatedNative.typeRef());
        assertEquals(4, updatedNative.amount());
        assertEquals("minecraft:stone:4", updatedNative.handle());
    }

    @Test
    void simpleAndNativeItemsCompareBySharedState() {
        RItem simple = RItem.builder()
            .material("minecraft:paper")
            .amount(1)
            .custom("payload", RNbtValue.byteArray(new byte[] {1, 2, 3}))
            .build();

        NativeRItem<String> nativeItem = NativeRItem.of(
            PlatformId.PAPER,
            "paper-handle",
            RItem.builder()
                .material("minecraft:paper")
                .amount(1)
                .custom("payload", RNbtValue.byteArray(new byte[] {1, 2, 3}))
                .build(),
            (currentHandle, _updatedItem) -> currentHandle
        );

        assertEquals(simple, nativeItem);
        assertEquals(simple.hashCode(), nativeItem.hashCode());
    }

    @Test
    void customModelDataCanBeClearedWithoutDroppingNativeBacking() {
        NativeRItem<String> item = NativeRItem.of(
            PlatformId.NEOFORGE,
            "cmd:7",
            RItem.builder()
                .material("minecraft:paper")
                .customModelData(7)
                .build(),
            (_currentHandle, updatedItem) -> updatedItem.customModelData()
                .map(modelData -> "cmd:" + modelData)
                .orElse("cleared")
        );

        RItem updated = item.withoutCustomModelData();
        NativeRItem<?> updatedNative = assertInstanceOf(NativeRItem.class, updated);

        assertTrue(updated.customModelData().isEmpty());
        assertEquals("cleared", updatedNative.handle());
    }
}
