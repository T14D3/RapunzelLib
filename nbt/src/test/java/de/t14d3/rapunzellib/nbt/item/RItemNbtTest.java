package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.attachments.RAttachmentKey;
import de.t14d3.rapunzellib.nbt.RNbtCodecs;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtPath;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.nbt.generated.RItemNbt;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RItemNbtTest {
    @Test
    void generatedItemSchemaProvidesTypedFieldAndFacadeCoverage() {
        RNbtCompound compound = RNbtCompound.empty();
        compound = RItemNbt.Fields.COMPONENTS.write(compound, RNbtCompound.empty());
        compound = RItemNbt.Paths.COMPONENTS_CUSTOM_NAME.write(compound, Component.text("Manual"));
        compound = RItemNbt.Paths.COMPONENTS_LORE.write(compound, List.of(Component.text("Line 1"), Component.text("Line 2")));
        compound = RItemNbt.Paths.COMPONENTS_DAMAGE.write(compound, 7);
        compound = RItemNbt.Paths.COMPONENTS_UNBREAKABLE.write(compound, true);
        compound = RItemNbt.Paths.COMPONENTS_CUSTOM_MODEL_DATA.write(compound, 11);
        compound = RItemNbt.Paths.COMPONENTS_REPAIR_COST.write(compound, 5);
        compound = RItemNbt.Paths.COMPONENTS_ENCHANTMENT_GLINT_OVERRIDE.write(compound, false);
        compound = RItemNbt.Fields.CUSTOM_DATA.write(compound, RNbtCompound.builder().putString("plugin", "demo").build());

        assertEquals("r_item", RItemNbt.NAME);
        assertSame(RItemNbt.Fields.COMPONENTS, RItemFields.COMPONENTS);
        assertSame(RItemNbt.Fields.CUSTOM_DATA, RItemFields.CUSTOM_DATA);
        assertSame(RItemNbt.Paths.COMPONENTS_CUSTOM_NAME, RItemFields.NAME);
        assertSame(RItemNbt.Paths.COMPONENTS_DAMAGE, RItemFields.DAMAGE);
        assertSame(RItemNbt.Paths.COMPONENTS_REPAIR_COST, RItemFields.REPAIR_COST);
        assertSame(RItemNbt.Paths.COMPONENTS_ENCHANTMENT_GLINT_OVERRIDE, RItemFields.ENCHANTMENT_GLINT_OVERRIDE);
        assertTrue(RItemFields.SCHEMA.fields().contains(RItemFields.COMPONENTS));
        assertTrue(RItemFields.SCHEMA.fields().contains(RItemFields.CUSTOM_DATA));
        assertEquals(Component.text("Manual"), RItemFields.NAME.read(compound).orElseThrow());
        assertEquals(2, RItemFields.LORE.read(compound).orElseThrow().size());
        assertEquals(7, RItemFields.DAMAGE.read(compound).orElseThrow());
        assertTrue(RItemFields.UNBREAKABLE.read(compound).orElseThrow());
        assertEquals(11, RItemFields.CUSTOM_MODEL_DATA.read(compound).orElseThrow());
        assertEquals(5, RItemFields.REPAIR_COST.read(compound).orElseThrow());
        assertFalse(RItemFields.ENCHANTMENT_GLINT_OVERRIDE.read(compound).orElseThrow());
        assertEquals("demo", RNbtCodecs.STRING.decode(RItemFields.CUSTOM_DATA.read(compound).orElseThrow().get("plugin").orElseThrow()));
    }

    @Test
    void highLevelFieldsMapIntoStructuredItemData() {
        RItem item = RItem.builder()
            .material("minecraft:paper")
            .amount(3)
            .name(Component.text("Manual"))
            .lore(List.of(Component.text("Line 1"), Component.text("Line 2")))
            .durability(7)
            .unbreakable(true)
            .customModelData(11)
            .repairCost(5)
            .enchantmentGlintOverride(Boolean.FALSE)
            .custom("plugin", RNbtValue.string("demo"))
            .build();

        assertEquals(Component.text("Manual"), item.name().orElseThrow());
        assertEquals(2, item.lore().size());
        assertEquals(7, item.durability());
        assertTrue(item.unbreakable());
        assertEquals(11, item.customModelData().orElseThrow());
        assertEquals(5, item.repairCost().orElseThrow());
        assertFalse(item.enchantmentGlintOverride().orElseThrow());
        assertEquals("demo", RNbtCodecs.STRING.decode(item.custom("plugin").orElseThrow()));
        assertEquals(RNbtCompound.class, item.data().get("components").orElseThrow().getClass());
        assertEquals(RNbtCompound.class, item.data().get("custom_data").orElseThrow().getClass());
    }

    @Test
    void itemsSupportTypedPathMutationWithoutRawSnapshots() {
        RNbtPath<String> ownerPath = RNbtPath.of(RNbtCodecs.STRING).key("custom_data").key("owner");

        RItem item = RItem.of("minecraft:book")
            .with(ownerPath, "Rapunzel")
            .withName(Component.text("Spellbook"))
            .withCustom("tier", RNbtValue.intValue(2));

        assertEquals("Rapunzel", item.get(ownerPath).orElseThrow());
        assertEquals(2, item.customData().get("tier").orElseThrow().asPrimitive().intValue());

        RItem updated = item.withName(null).withoutCustom("owner");

        assertTrue(updated.name().isEmpty());
        assertFalse(updated.custom("owner").isPresent());
        assertEquals(2, updated.customData().get("tier").orElseThrow().asPrimitive().intValue());
    }

    @Test
    void repairCostAndGlintOverrideSupportNullableMutationHelpers() {
        RItem item = RItem.of("minecraft:book")
            .withRepairCost(9)
            .withEnchantmentGlintOverride(Boolean.TRUE);

        assertEquals(9, item.repairCost().orElseThrow());
        assertTrue(item.enchantmentGlintOverride().orElseThrow());

        RItem updated = item.withRepairCost(null).withEnchantmentGlintOverride(null);

        assertTrue(updated.repairCost().isEmpty());
        assertTrue(updated.enchantmentGlintOverride().isEmpty());
    }

    @Test
    void itemAttachmentsReuseCustomDataPayload() {
        RAttachmentKey<String> ownerKey = RAttachmentKey.persistent("test:owner", String.class);

        RItem item = RItem.of("minecraft:paper")
            .withAttachment(ownerKey, "Rapunzel")
            .withCustom("plugin", RNbtValue.string("demo"));

        assertTrue(item.supportsAttachment(ownerKey));
        assertEquals("Rapunzel", item.attachment(ownerKey).orElseThrow());
        assertEquals("demo", RNbtCodecs.STRING.decode(item.custom("plugin").orElseThrow()));
        assertEquals("Rapunzel", RNbtCodecs.STRING.decode(
            item.customData().get(RItemAttachments.ROOT_KEY).orElseThrow().asCompound().get("test:owner").orElseThrow()
        ));

        RItem updated = item.withoutAttachment(ownerKey);

        assertTrue(updated.attachment(ownerKey).isEmpty());
        assertEquals("demo", RNbtCodecs.STRING.decode(updated.custom("plugin").orElseThrow()));
    }
}
