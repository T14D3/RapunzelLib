package de.t14d3.rapunzellib.nbt.sponge;

import de.t14d3.rapunzellib.nbt.BlockEntityNbtSerializer;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.SerializationException;
import de.t14d3.rapunzellib.nbt.SerializedBlockEntity;
import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.BlockEntityArchetype;
import org.spongepowered.api.block.entity.BlockEntityType;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import java.time.Instant;
import java.util.Objects;

/**
 * Sponge implementation of {@link BlockEntityNbtSerializer} that round-trips
 * block entity NBT via Sponge's {@link BlockEntityArchetype} data container.
 *
 * @param <L> the block entity location type
 */
public final class SpongeBlockEntityNbtSerializer<L extends SpongeLocation> implements BlockEntityNbtSerializer<BlockEntity, L> {

    @Override
    public @NotNull SerializedBlockEntity serialize(@NotNull BlockEntity blockEntity) {
        Objects.requireNonNull(blockEntity, "blockEntity");
        try {
            RKey blockEntityType = typeKey(blockEntity);
            DataContainer container = blockEntity.createArchetype().blockEntityData();
            RNbtCompound data = SpongeNbtDataSupport.toTree(container);
            return new SerializedBlockEntity(blockEntityType, data, Instant.now());
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize block entity: " + blockEntity, e);
        }
    }

    @Override
    public @NotNull BlockEntity deserialize(@NotNull SerializedBlockEntity data, @NotNull L location) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(location, "location");
        try {
            if (!(location.world() instanceof ServerWorld world)) {
                throw new SerializationException("Block entity deserialization requires a server world");
            }

            BlockEntityType type = resolveType(data.blockEntityType());
            DataContainer container = SpongeNbtDataSupport.fromTree(data.data());

            BlockEntityArchetype archetype = BlockEntityArchetype.builder().blockEntity(type).build();
            archetype.setRawData(container);

            ServerLocation serverLocation = world.location(location.x(), location.y(), location.z());
            return archetype.apply(serverLocation)
                .orElseThrow(() -> new SerializationException(
                    "Failed to create block entity instance: " + data.blockEntityTypeId()));
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize block entity: " + data.blockEntityTypeId(), e);
        }
    }

    private static @NotNull RKey typeKey(@NotNull BlockEntity blockEntity) {
        var key = blockEntity.type().key(RegistryTypes.BLOCK_ENTITY_TYPE);
        return RKey.of(key.namespace(), key.value());
    }

    private static @NotNull BlockEntityType resolveType(@NotNull RKey key) {
        return BlockEntityTypes.byKey(key).orElseThrow(() ->
            new SerializationException("Unknown block entity type: " + key.asString()));
    }

    /** Small helper to resolve a Sponge block entity type by resource key. */
    private static final class BlockEntityTypes {
        private BlockEntityTypes() {
        }

        static java.util.Optional<BlockEntityType> byKey(@NotNull RKey key) {
            return org.spongepowered.api.Sponge.server().registry(RegistryTypes.BLOCK_ENTITY_TYPE)
                .findValue(org.spongepowered.api.ResourceKey.resolve(key.asString()));
        }
    }
}
