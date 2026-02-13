package de.t14d3.rapunzellib.nbt.sponge;

import de.t14d3.rapunzellib.nbt.NbtSerializer;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtList;
import de.t14d3.rapunzellib.nbt.RNbtType;
import de.t14d3.rapunzellib.nbt.SerializationException;
import de.t14d3.rapunzellib.nbt.SerializedEntity;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryHandles;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpongeNbtSerializer implements NbtSerializer<Entity, SpongeLocation> {
    @Override
    public @NotNull SerializedEntity serialize(@NotNull Entity entity) {
        try {
            DataContainer container = entity.toContainer();
            RNbtCompound data = stripLocationData(SpongeNbtDataSupport.toTree(container));

            List<SerializedEntity> passengers = new ArrayList<>();
            for (Entity passenger : entity.passengers()) {
                passengers.add(serialize(passenger));
            }

            return new SerializedEntity(
                REntityType.ref(entity.type().key(RegistryTypes.ENTITY_TYPE).asString()),
                data,
                passengers,
                entity.uniqueId(),
                Instant.now(),
                RNbtCompound.empty()
            );
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize entity: " + entity.type(), e);
        }
    }

    @Override
    public @NotNull Entity deserialize(@NotNull SerializedEntity data, @NotNull SpongeLocation location) {
        try {
            DataContainer container = SpongeNbtDataSupport.fromTree(withLocation(data.data(), location));

            EntityType<?> entityType = RRegistryHandles.find(data.entityType(), EntityType.class)
                .or(() -> Sponge.server().registry(RegistryTypes.ENTITY_TYPE)
                    .findValue(org.spongepowered.api.ResourceKey.resolve(data.entityTypeId())))
                .orElseThrow(() -> new SerializationException("Unknown entity type: " + data.entityTypeId()));

            ServerWorld world = (ServerWorld) location.world();
            ServerLocation spawnLocation = world.location(location.x(), location.y(), location.z());

            Entity spawned = spawn(entityType, world, spawnLocation, container, data.entityTypeId());
            for (SerializedEntity passengerData : data.passengers()) {
                spawned.passengers().add(deserialize(passengerData, location));
            }
            return spawned;
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize entity: " + data.entityTypeId(), e);
        }
    }

    private static @NotNull Entity spawn(
        @NotNull EntityType<?> entityType,
        @NotNull ServerWorld world,
        @NotNull ServerLocation spawnLocation,
        @NotNull DataContainer container,
        @NotNull String entityTypeKey
    ) {
        EntityArchetype archetype = EntityArchetype.of(entityType);
        try {
            archetype.setRawData(container);
            Optional<Entity> created = archetype.apply(spawnLocation);
            if (created.isPresent()) {
                return created.get();
            }
        } catch (InvalidDataException ignored) {
        }

        Entity entity = world.createEntity(entityType, new Vector3d(spawnLocation.x(), spawnLocation.y(), spawnLocation.z()));
        if (entity == null) {
            throw new SerializationException("Failed to create entity instance: " + entityTypeKey);
        }

        try {
            entity.setRawData(container);
        } catch (InvalidDataException e) {
            throw new SerializationException("Entity rejected raw data during setRawData: " + entityTypeKey, e);
        }

        world.spawnEntity(entity);
        return entity;
    }

    private static @NotNull RNbtCompound stripLocationData(@NotNull RNbtCompound data) {
        return data
            .remove("Pos")
            .remove("Rotation")
            .remove("WorldUUID");
    }

    private static @NotNull RNbtCompound withLocation(@NotNull RNbtCompound data, @NotNull SpongeLocation location) {
        return data
            .put("Pos", position(location))
            .put("Rotation", rotation(location));
    }

    private static @NotNull RNbtList position(@NotNull SpongeLocation location) {
        return RNbtList.builder(RNbtType.DOUBLE)
            .addDouble(location.x())
            .addDouble(location.y())
            .addDouble(location.z())
            .build();
    }

    private static @NotNull RNbtList rotation(@NotNull SpongeLocation location) {
        return RNbtList.builder(RNbtType.DOUBLE)
            .addDouble(location.yaw())
            .addDouble(location.pitch())
            .build();
    }
}
