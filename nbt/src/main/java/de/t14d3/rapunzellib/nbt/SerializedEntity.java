package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An immutable, serializable snapshot of a Minecraft entity.
 * <p>
 * Captures the entity type, NBT data, passenger list, original UUID,
 * serialization timestamp, and optional metadata.</p>
 *
 * @param entityType  the entity type reference
 * @param data        the entity's NBT data compound
 * @param passengers  the list of passenger entities (recursive)
 * @param originalUuid the original entity UUID
 * @param serializedAt the timestamp when this snapshot was created
 * @param metadata    additional metadata associated with this serialization
 */
public record SerializedEntity(
    @NotNull RRegistryRef<REntityType> entityType,
    @NotNull RNbtCompound data,
    @NotNull List<SerializedEntity> passengers,
    @NotNull UUID originalUuid,
    @NotNull Instant serializedAt,
    @NotNull RNbtCompound metadata
) implements Serializable {

    public SerializedEntity {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(data, "data");
        passengers = List.copyOf(Objects.requireNonNull(passengers, "passengers"));
        Objects.requireNonNull(originalUuid, "originalUuid");
        Objects.requireNonNull(serializedAt, "serializedAt");
        Objects.requireNonNull(metadata, "metadata");
    }

    /**
     * Creates a serialized entity from a string entity type, with empty metadata.
     *
     * @param entityType   the entity type as a string
     * @param data         the NBT data
     * @param passengers   the passengers
     * @param originalUuid the original UUID
     * @param serializedAt the serialization timestamp
     */
    public SerializedEntity(
        @NotNull String entityType,
        @NotNull RNbtCompound data,
        @NotNull List<SerializedEntity> passengers,
        @NotNull UUID originalUuid,
        @NotNull Instant serializedAt
    ) {
        this(REntityType.ref(entityType), data, passengers, originalUuid, serializedAt, RNbtCompound.empty());
    }

    /**
     * Creates a serialized entity from a string entity type with metadata.
     *
     * @param entityType   the entity type as a string
     * @param data         the NBT data
     * @param passengers   the passengers
     * @param originalUuid the original UUID
     * @param serializedAt the serialization timestamp
     * @param metadata     additional metadata
     */
    public SerializedEntity(
        @NotNull String entityType,
        @NotNull RNbtCompound data,
        @NotNull List<SerializedEntity> passengers,
        @NotNull UUID originalUuid,
        @NotNull Instant serializedAt,
        @NotNull RNbtCompound metadata
    ) {
        this(REntityType.ref(entityType), data, passengers, originalUuid, serializedAt, metadata);
    }

    /**
     * Creates a simple serialized entity with no passengers, current timestamp, and no metadata.
     *
     * @param entityType   the entity type as a string
     * @param data         the NBT data
     * @param originalUuid the original UUID
     * @return a new serialized entity
     */
    public static @NotNull SerializedEntity of(
        @NotNull String entityType,
        @NotNull RNbtCompound data,
        @NotNull UUID originalUuid
    ) {
        return new SerializedEntity(REntityType.ref(entityType), data, List.of(), originalUuid, Instant.now(), RNbtCompound.empty());
    }

    /**
     * Returns the entity type as an {@link RKey}.
     *
     * @return the entity type key
     */
    public @NotNull RKey entityTypeKey() {
        return entityType.key();
    }

    /**
     * Returns the entity type as a string ID.
     *
     * @return the entity type string
     */
    public @NotNull String entityTypeId() {
        return entityTypeKey().asString();
    }

    /**
     * Serializes this entity snapshot to a Base64-encoded string.
     *
     * @return the Base64 string
     * @throws SerializationException if serialization fails
     */
    public String toBase64() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ObjectOutputStream objectOutput = new ObjectOutputStream(output)) {
                objectOutput.writeObject(this);
            }
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize entity transport payload", e);
        }
    }

    /**
     * Deserializes a Base64-encoded string back into a {@link SerializedEntity}.
     *
     * @param data the Base64 string
     * @return the deserialized entity
     * @throws SerializationException if deserialization fails
     */
    public static @NotNull SerializedEntity fromBase64(@NotNull String data) {
        Objects.requireNonNull(data, "data");
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (SerializedEntity) objectInput.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Failed to deserialize entity transport payload", e);
        }
    }

    /**
     * Returns a new snapshot with additional metadata written via the given field.
     *
     * @param <T>   the metadata value type
     * @param field the field to write
     * @param value the value to write
     * @return a new serialized entity with updated metadata
     */
    public <T> @NotNull SerializedEntity withMetadata(@NotNull RNbtField<T> field, @NotNull T value) {
        return new SerializedEntity(entityType, data, passengers, originalUuid, serializedAt, field.write(metadata, value));
    }

    /**
     * Returns a new snapshot with the NBT data replaced.
     *
     * @param data the new NBT data
     * @return a new serialized entity
     */
    public @NotNull SerializedEntity withData(@NotNull RNbtCompound data) {
        return new SerializedEntity(entityType, Objects.requireNonNull(data, "data"), passengers, originalUuid, serializedAt, metadata);
    }

    @Override
    public String toString() {
        return "SerializedEntity[entityType=" + entityTypeId() + ", dataKeys=" + data.keys() + ", passengers=" + passengers.size() + ", originalUuid=" + originalUuid + ']';
    }
}
