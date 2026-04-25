package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.BlockEntityNbtSerializer;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.SerializationException;
import de.t14d3.rapunzellib.nbt.SerializedBlockEntity;
import de.t14d3.rapunzellib.nbt.generated.BlockEntityRootNbt;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;

public final class SharedBlockEntityNbtSerializerCore<L extends SharedBlockEntityLocation> implements BlockEntityNbtSerializer<BlockEntity, L> {
    private static final HolderLookup.Provider FALLBACK_REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();

    @Override
    public @NotNull SerializedBlockEntity serialize(@NotNull BlockEntity blockEntity) {
        try {
            RKey blockEntityType = typeKey(blockEntity.getType());
            RNbtCompound data = SharedNbtIoSupport.toTree(blockEntity.saveWithFullMetadata(registries(blockEntity)));
            return new SerializedBlockEntity(blockEntityType, data, Instant.now());
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize block entity: " + blockEntityName(blockEntity), e);
        }
    }

    @Override
    public @NotNull BlockEntity deserialize(@NotNull SerializedBlockEntity data, @NotNull L location) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(location, "location");

        try {
            RNbtCompound tree = BlockEntityRootNbt.Fields.ID.write(data.data(), data.blockEntityTypeId());
            tree = BlockEntityRootNbt.Fields.X.write(tree, location.x());
            tree = BlockEntityRootNbt.Fields.Y.write(tree, location.y());
            tree = BlockEntityRootNbt.Fields.Z.write(tree, location.z());

            CompoundTag nbt = SharedNbtIoSupport.fromTree(tree);
            BlockEntity blockEntity = BlockEntity.loadStatic(location.pos(), location.state(), nbt, location.registries());
            if (blockEntity == null) {
                throw new SerializationException("Failed to deserialize block entity: " + data.blockEntityTypeId());
            }
            return blockEntity;
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize block entity: " + data.blockEntityTypeId(), e);
        }
    }

    private static @NotNull RKey typeKey(@NotNull BlockEntityType<?> type) {
        Identifier key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(Objects.requireNonNull(type, "type"));
        if (key == null) {
            throw new SerializationException("Unknown block entity type: " + type);
        }
        return RKey.of(key.toString());
    }

    private static @NotNull HolderLookup.Provider registries(@NotNull BlockEntity blockEntity) {
        return blockEntity.getLevel() == null ? FALLBACK_REGISTRIES : blockEntity.getLevel().registryAccess();
    }

    private static @NotNull String blockEntityName(@NotNull BlockEntity blockEntity) {
        return typeKey(blockEntity.getType()).asString();
    }
}
