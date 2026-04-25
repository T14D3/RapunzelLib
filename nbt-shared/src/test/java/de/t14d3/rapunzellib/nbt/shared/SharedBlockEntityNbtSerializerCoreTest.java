package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.SerializedBlockEntity;
import de.t14d3.rapunzellib.nbt.generated.BlockEntityRootNbt;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

final class SharedBlockEntityNbtSerializerCoreTest {
    private static final HolderLookup.Provider REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();

    @Test
    void serializesCanonicalRootDataAndDeserializesAgainstTargetLocation() {
        SharedBlockEntityNbtSerializerCore<TestLocation> serializer = new SharedBlockEntityNbtSerializerCore<>();
        ChestBlockEntity chest = new ChestBlockEntity(new BlockPos(12, 64, -4), Blocks.CHEST.defaultBlockState());
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        try {
            Field name = BaseContainerBlockEntity.class.getDeclaredField("name");
            name.setAccessible(true);
            name.set(chest, net.minecraft.network.chat.Component.literal("Vault"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        SerializedBlockEntity serialized = serializer.serialize(chest);

        assertEquals("minecraft:chest", serialized.blockEntityTypeId());
        assertEquals(12, BlockEntityRootNbt.Fields.X.read(serialized.data()).orElseThrow());
        assertEquals(64, BlockEntityRootNbt.Fields.Y.read(serialized.data()).orElseThrow());
        assertEquals(-4, BlockEntityRootNbt.Fields.Z.read(serialized.data()).orElseThrow());
        assertEquals(Component.text("Vault"), BlockEntityRootNbt.Fields.CUSTOM_NAME.read(serialized.data()).orElseThrow());
        assertFalse(BlockEntityRootNbt.Fields.ITEMS.read(serialized.data()).orElseThrow().isEmpty());

        BlockState targetState = Blocks.CHEST.defaultBlockState();
        BlockEntity deserialized = serializer.deserialize(
            serialized,
            new TestLocation(new BlockPos(20, 70, 5), targetState, REGISTRIES)
        );

        ChestBlockEntity restored = assertInstanceOf(ChestBlockEntity.class, deserialized);
        assertEquals(new BlockPos(20, 70, 5), restored.getBlockPos());
        assertEquals(Items.DIAMOND, restored.getItem(0).getItem());
        assertEquals(3, restored.getItem(0).getCount());
        assertEquals("Vault", restored.getCustomName().getString());
    }

    private record TestLocation(
        BlockPos pos,
        BlockState state,
        HolderLookup.Provider registries
    ) implements SharedBlockEntityLocation {
    }
}
