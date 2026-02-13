package de.t14d3.rapunzellib.nbt.fabric;

import de.t14d3.rapunzellib.nbt.RNbtCodecs;
import de.t14d3.rapunzellib.nbt.item.NativeRItem;
import de.t14d3.rapunzellib.nbt.item.RItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricItemStackAdapterTest {
    private final FabricItemStackAdapter adapter = new FabricItemStackAdapter();

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void richComponentsRoundTripWithoutPlainTextLoss() {
        Component name = Component.text()
            .append(Component.text("Hello", NamedTextColor.RED))
            .append(Component.text(" world").decorate(TextDecoration.BOLD))
            .build();

        RItem roundTrip = adapter.snapshot(
            adapter.create(RItem.builder().material("minecraft:paper").name(name).build())
        );

        assertEquals(
            GsonComponentSerializer.gson().serialize(name),
            GsonComponentSerializer.gson().serialize(roundTrip.name().orElseThrow())
        );
    }

    @Test
    void nativeUpdatesPreserveRepairCostAndClearCustomModelData() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        CompoundTag customData = new CompoundTag();
        customData.putString("owner", "Rapunzel");
        stack.set(DataComponents.REPAIR_COST, 7);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(99)));
        stack.setDamageValue(4);

        RItem item = adapter.snapshot(stack);
        RItem updated = item.withAmount(3).withoutCustomModelData();
        NativeRItem<?> updatedNative = assertInstanceOf(NativeRItem.class, updated);
        ItemStack updatedStack = assertInstanceOf(ItemStack.class, updatedNative.handle());

        assertEquals(4, item.durability());
        assertEquals(7, item.repairCost().orElseThrow());
        assertFalse(item.enchantmentGlintOverride().orElseThrow());
        assertEquals("Rapunzel", RNbtCodecs.STRING.decode(item.customData().get("owner").orElseThrow()));
        assertEquals(7, updatedStack.get(DataComponents.REPAIR_COST));
        assertFalse(updatedStack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertEquals(3, updatedStack.getCount());
        assertFalse(updated.customModelData().isPresent());
        assertNull(updatedStack.get(DataComponents.CUSTOM_MODEL_DATA));
        assertEquals("Rapunzel", updatedStack.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("owner", ""));
        assertEquals(4, updatedStack.getDamageValue());
    }

    @Test
    void sharedRepairCostAndGlintOverrideRoundTripThroughSharedAdapter() {
        RItem item = RItem.builder()
            .material("minecraft:diamond_sword")
            .repairCost(12)
            .enchantmentGlintOverride(Boolean.TRUE)
            .build();

        ItemStack nativeStack = adapter.create(item);
        RItem roundTrip = adapter.snapshot(nativeStack);

        assertEquals(12, nativeStack.get(DataComponents.REPAIR_COST));
        assertTrue(nativeStack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertEquals(12, roundTrip.repairCost().orElseThrow());
        assertTrue(roundTrip.enchantmentGlintOverride().orElseThrow());
    }
}
