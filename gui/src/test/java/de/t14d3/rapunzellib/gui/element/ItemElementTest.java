package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RKey;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ItemElementTest {
    @Test
    void builderStoresRItemPayload() {
        RItem item = RItem.builder()
            .typeKey(RKey.of("minecraft:stone"))
            .amount(2)
            .name(Component.text("Stone"))
            .build();

        ItemElement element = ItemElement.builder()
            .item(item)
            .tooltip(Component.text("Tooltip"))
            .build();

        assertSame(item, element.item());
        assertEquals(Component.text("Tooltip"), element.tooltip());
    }
}
