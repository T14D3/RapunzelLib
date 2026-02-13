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

    public SerializedBlockEntity(
        @NotNull String blockEntityType,
        @NotNull RNbtCompound data,
        @NotNull Instant serializedAt
    ) {
        this(RKey.of(blockEntityType), data, serializedAt, RNbtCompound.empty());
    }

    public SerializedBlockEntity(
        @NotNull RKey blockEntityType,
        @NotNull RNbtCompound data,
        @NotNull Instant serializedAt
    ) {
        this(blockEntityType, data, serializedAt, RNbtCompound.empty());
    }

    public SerializedBlockEntity(
        @NotNull String blockEntityType,
        @NotNull RNbtCompound data,
        @NotNull Instant serializedAt,
        @NotNull RNbtCompound metadata
    ) {
        this(RKey.of(blockEntityType), data, serializedAt, metadata);
    }

    public static @NotNull SerializedBlockEntity of(
        @NotNull String blockEntityType,
        @NotNull RNbtCompound data
    ) {
        return new SerializedBlockEntity(RKey.of(blockEntityType), data, Instant.now(), RNbtCompound.empty());
    }

    public @NotNull String blockEntityTypeId() {
        return blockEntityType.asString();
    }

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

    public <T> @NotNull SerializedBlockEntity withMetadata(@NotNull RNbtField<T> field, @NotNull T value) {
        return new SerializedBlockEntity(blockEntityType, data, serializedAt, field.write(metadata, value));
    }

    public @NotNull SerializedBlockEntity withData(@NotNull RNbtCompound data) {
        return new SerializedBlockEntity(blockEntityType, Objects.requireNonNull(data, "data"), serializedAt, metadata);
    }

    @Override
    public @NotNull String toString() {
        return "SerializedBlockEntity[blockEntityType=" + blockEntityTypeId() + ", dataKeys=" + data.keys() + ']';
    }
}
