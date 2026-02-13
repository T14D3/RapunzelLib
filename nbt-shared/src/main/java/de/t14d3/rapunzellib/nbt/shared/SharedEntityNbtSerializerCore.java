package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.NbtSerializer;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.SerializationException;
import de.t14d3.rapunzellib.nbt.SerializedEntity;
import de.t14d3.rapunzellib.nbt.shared.generated.SharedEntityRootNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
            RNbtCompound tree = SharedEntityRootNbt.Fields.POS.write(data.data(), position(location));
            tree = SharedEntityRootNbt.Fields.ROTATION.write(tree, rotation(location));
            CompoundTag nbt = SharedNbtIoSupport.fromTree(tree);

            ServerLevel level = location.level();
            ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), nbt);
            Optional<Entity> spawned = EntityType.create(input, level, EntitySpawnReason.LOAD);

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

    protected final @NotNull RNbtCompound stripLocationData(@NotNull RNbtCompound nbt) {
        return Objects.requireNonNull(nbt, "nbt")
            .remove(SharedEntityRootNbt.Paths.POS)
            .remove(SharedEntityRootNbt.Paths.POS_X)
            .remove(SharedEntityRootNbt.Paths.POS_Y)
            .remove(SharedEntityRootNbt.Paths.POS_Z)
            .remove(SharedEntityRootNbt.Paths.ROTATION)
            .remove(SharedEntityRootNbt.Paths.ROTATION_YAW)
            .remove(SharedEntityRootNbt.Paths.ROTATION_PITCH)
            .remove(SharedEntityRootNbt.Paths.WORLD_UUID_LEAST)
            .remove(SharedEntityRootNbt.Paths.WORLD_UUID_MOST);
    }

    private static @NotNull List<Double> position(@NotNull SharedEntityLocation location) {
        return List.of(location.x(), location.y(), location.z());
    }

    private static @NotNull List<Float> rotation(@NotNull SharedEntityLocation location) {
        return List.of(location.yaw(), location.pitch());
    }
}
