package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * An immutable, serializable snapshot of a Minecraft block entity.
 * <p>
 * Captures the block entity type, NBT data, serialization timestamp, and optional metadata.</p>
 *
 * @param blockEntityType the block entity type key
 * @param data            the NBT data compound
 * @param serializedAt    the timestamp when this snapshot was created
 * @param metadata        additional metadata associated with this serialization
 */
public record SerializedBlockEntity(
    @NotNull RKey blockEntityType,
    @NotNull RNbtCompound data,
    @NotNull Instant serializedAt,
    @NotNull RNbtCompound metadata
) implements Serializable {

    public SerializedBlockEntity {
        Objects.requireNonNull(blockEntityType, "blockEntityType");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(serializedAt, "serializedAt");
        Objects.requireNonNull(metadata, "metadata");
    }

    /**
     * Creates a block entity snapshot from a string type, with no metadata.
     *
     * @param blockEntityType the block entity type as a string
     * @param data            the NBT data
     * @param serializedAt    the serialization timestamp
     */
    public SerializedBlockEntity(
        @NotNull String blockEntityType,
        @NotNull RNbtCompound data,
        @NotNull Instant serializedAt
    ) {
        this(RKey.of(blockEntityType), data, serializedAt, RNbtCompound.empty());
    }

    /**
     * Creates a block entity snapshot from a key, with no metadata.
     *
     * @param blockEntityType the block entity type key
     * @param data            the NBT data
     * @param serializedAt    the serialization timestamp
     */
    public SerializedBlockEntity(
        @NotNull RKey blockEntityType,
        @NotNull RNbtCompound data,
        @NotNull Instant serializedAt
    ) {
        this(blockEntityType, data, serializedAt, RNbtCompound.empty());
    }

    /**
     * Creates a block entity snapshot from a string type with metadata.
     *
     * @param blockEntityType the block entity type as a string
     * @param data            the NBT data
     * @param serializedAt    the serialization timestamp
     * @param metadata        additional metadata
     */
    public SerializedBlockEntity(
        @NotNull String blockEntityType,
        @NotNull RNbtCompound data,
        @NotNull Instant serializedAt,
        @NotNull RNbtCompound metadata
    ) {
        this(RKey.of(blockEntityType), data, serializedAt, metadata);
    }

    /**
     * Creates a simple block entity snapshot with current timestamp and no metadata.
     *
     * @param blockEntityType the block entity type as a string
     * @param data            the NBT data
     * @return a new serialized block entity
     */
    public static @NotNull SerializedBlockEntity of(
        @NotNull String blockEntityType,
        @NotNull RNbtCompound data
    ) {
        return new SerializedBlockEntity(RKey.of(blockEntityType), data, Instant.now(), RNbtCompound.empty());
    }

    /**
     * Returns the block entity type as a string ID.
     *
     * @return the block entity type string
     */
    public @NotNull String blockEntityTypeId() {
        return blockEntityType.asString();
    }

    /**
     * Serializes this block entity snapshot to a Base64-encoded string.
     *
     * @return the Base64 string
     * @throws SerializationException if serialization fails
     */
    public @NotNull String toBase64() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ObjectOutputStream objectOutput = new ObjectOutputStream(output)) {
                objectOutput.writeObject(this);
            }
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize block entity transport payload", e);
        }
    }

    /**
     * Deserializes a Base64-encoded string back into a {@link SerializedBlockEntity}.
     *
     * @param data the Base64 string
     * @return the deserialized block entity
     * @throws SerializationException if deserialization fails
     */
    public static @NotNull SerializedBlockEntity fromBase64(@NotNull String data) {
        Objects.requireNonNull(data, "data");
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (SerializedBlockEntity) objectInput.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Failed to deserialize block entity transport payload", e);
        }
    }

    /**
     * Returns a new snapshot with additional metadata written via the given field.
     *
     * @param <T>   the metadata value type
     * @param field the field to write
     * @param value the value to write
     * @return a new serialized block entity with updated metadata
     */
    public <T> @NotNull SerializedBlockEntity withMetadata(@NotNull RNbtField<T> field, @NotNull T value) {
        return new SerializedBlockEntity(blockEntityType, data, serializedAt, field.write(metadata, value));
    }

    /**
     * Returns a new snapshot with the NBT data replaced.
     *
     * @param data the new NBT data
     * @return a new serialized block entity
     */
    public @NotNull SerializedBlockEntity withData(@NotNull RNbtCompound data) {
        return new SerializedBlockEntity(blockEntityType, Objects.requireNonNull(data, "data"), serializedAt, metadata);
    }

    @Override
    public @NotNull String toString() {
        return "SerializedBlockEntity[blockEntityType=" + blockEntityTypeId() + ", dataKeys=" + data.keys() + ']';
    }
}
