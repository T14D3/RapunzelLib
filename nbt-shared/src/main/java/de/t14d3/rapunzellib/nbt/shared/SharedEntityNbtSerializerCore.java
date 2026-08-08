package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.NbtSerializer;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.SerializationException;
import de.t14d3.rapunzellib.nbt.SerializedEntity;
import de.t14d3.rapunzellib.nbt.generated.EntityRootNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
// #if VERSION >= 26.2
import net.minecraft.world.entity.EntitySpawnRequest;
// #endif
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared serializer for Minecraft entities.
 * <p>
 * Serializes entity NBT data (stripping location fields) and deserializes
 * it back, re-injecting position and rotation from the
 * {@link SharedEntityLocation}. Supports passenger entities recursively.
 *
 * @param <L> the entity location type
 */
public final class SharedEntityNbtSerializerCore<L extends SharedEntityLocation> implements NbtSerializer<Entity, L> {
    @Override
    public final @NotNull SerializedEntity serialize(@NotNull Entity entity) {
        try {
            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            entity.saveWithoutId(output);
            RNbtCompound data = stripLocationData(SharedNbtIoSupport.toTree(output.buildResult()));

            List<SerializedEntity> passengers = new ArrayList<>();
            for (Entity passenger : entity.getPassengers()) {
                passengers.add(serialize(passenger));
            }

            return new SerializedEntity(
                EntityType.getKey(entity.getType()).toString(),
                data,
                passengers,
                entity.getUUID(),
                Instant.now()
            );
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize entity: " + entity.getType(), e);
        }
    }

    @Override
    public final @NotNull Entity deserialize(@NotNull SerializedEntity data, @NotNull L location) {
        try {
            RNbtCompound tree = EntityRootNbt.Fields.POS.write(data.data(), position(location));
            tree = EntityRootNbt.Fields.ROTATION.write(tree, rotation(location));
            // Serialize writes the entity NBT via Entity.saveWithoutId, which omits the
            // "id" key, but EntityType.create resolves the type solely from the NBT's
            // "id" string (input.read("id", CODEC)); without it the spawn is skipped as
            // "Skipping Entity with id [invalid]". Re-inject the id carried by the
            // transport-level entity type id, like Pos/Rotation above.
            tree = EntityRootNbt.Fields.ID.write(tree, data.entityTypeId());
            CompoundTag nbt = SharedNbtIoSupport.fromTree(tree);

            ServerLevel level = location.level();
            ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), nbt);
            // #if VERSION >= 26.2
            Optional<Entity> spawned = EntityType.create(input, level, new EntitySpawnRequest(EntitySpawnReason.LOAD, false));
            // #else
            Optional<Entity> spawned = EntityType.create(input, level, EntitySpawnReason.LOAD);
            // #endif

            if (spawned.isEmpty()) {
                throw new SerializationException("Failed to spawn entity: " + data.entityTypeId());
            }

            Entity nmsEntity = spawned.get();
            level.addFreshEntity(nmsEntity);

            for (SerializedEntity passengerData : data.passengers()) {
                Entity passenger = deserialize(passengerData, location);
                passenger.startRiding(nmsEntity);
            }

            return nmsEntity;
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize entity: " + data.entityTypeId(), e);
        }
    }

    private @NotNull RNbtCompound stripLocationData(@NotNull RNbtCompound nbt) {
        return Objects.requireNonNull(nbt, "nbt")
            .remove(EntityRootNbt.Paths.POS)
            .remove(EntityRootNbt.Paths.POS_X)
            .remove(EntityRootNbt.Paths.POS_Y)
            .remove(EntityRootNbt.Paths.POS_Z)
            .remove(EntityRootNbt.Paths.ROTATION)
            .remove(EntityRootNbt.Paths.ROTATION_YAW)
            .remove(EntityRootNbt.Paths.ROTATION_PITCH)
            .remove(EntityRootNbt.Paths.WORLD_UUID_LEAST)
            .remove(EntityRootNbt.Paths.WORLD_UUID_MOST);
    }

    private static @NotNull List<Double> position(@NotNull SharedEntityLocation location) {
        return List.of(location.x(), location.y(), location.z());
    }

    private static @NotNull List<Float> rotation(@NotNull SharedEntityLocation location) {
        return List.of(location.yaw(), location.pitch());
    }
}
