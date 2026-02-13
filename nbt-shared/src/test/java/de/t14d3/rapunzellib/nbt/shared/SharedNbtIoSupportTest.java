package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.RNbtCodecs;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class SharedNbtIoSupportTest {
    @Test
    void convertsCompoundTagsIntoStructuredRnbtTrees() {
        CompoundTag nativeTag = new CompoundTag();
        nativeTag.putString("id", "minecraft:paper");
        nativeTag.putInt("count", 3);
        nativeTag.putByteArray("payload", new byte[] {1, 2, 3});

        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf("Line 1"));
        lore.add(StringTag.valueOf("Line 2"));
        nativeTag.put("lore", lore);

        RNbtCompound tree = SharedNbtIoSupport.toTree(nativeTag);

        assertEquals("minecraft:paper", RNbtCodecs.STRING.decode(tree.get("id").orElseThrow()));
        assertEquals(3, RNbtCodecs.INT.decode(tree.get("count").orElseThrow()));
        assertArrayEquals(new byte[] {1, 2, 3}, RNbtCodecs.BYTE_ARRAY.decode(tree.get("payload").orElseThrow()));
        assertEquals(RNbtType.STRING, tree.get("lore").orElseThrow().asList().elementType());
    }

    @Test
    void roundTripsCompressedTreesThroughSharedIo() {
        RNbtCompound original = RNbtCompound.builder()
            .putString("entity", "minecraft:zombie")
            .putInt("Health", 20)
            .put("Pos", de.t14d3.rapunzellib.nbt.RNbtList.builder(RNbtType.DOUBLE).addDouble(1.0).addDouble(2.0).addDouble(3.0).build())
            .build();

        byte[] bytes = SharedNbtIoSupport.serializeCompressed(original);
        RNbtCompound restored = SharedNbtIoSupport.deserializeCompressed(bytes);

        assertEquals(original, restored);
        assertEquals(original, SharedNbtIoSupport.toTree(SharedNbtIoSupport.fromTree(original)));
    }
}
