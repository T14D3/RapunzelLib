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

    public SerializedEntity(
        @NotNull String entityType,
        @NotNull RNbtCompound data,
        @NotNull List<SerializedEntity> passengers,
        @NotNull UUID originalUuid,
        @NotNull Instant serializedAt
    ) {
        this(REntityType.ref(entityType), data, passengers, originalUuid, serializedAt, RNbtCompound.empty());
    }

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

    public static @NotNull SerializedEntity of(
        @NotNull String entityType,
        @NotNull RNbtCompound data,
        @NotNull UUID originalUuid
    ) {
        return new SerializedEntity(REntityType.ref(entityType), data, List.of(), originalUuid, Instant.now(), RNbtCompound.empty());
    }

    public @NotNull RKey entityTypeKey() {
        return entityType.key();
    }

    public @NotNull String entityTypeId() {
        return entityTypeKey().asString();
    }

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

    public <T> @NotNull SerializedEntity withMetadata(@NotNull RNbtField<T> field, @NotNull T value) {
        return new SerializedEntity(entityType, data, passengers, originalUuid, serializedAt, field.write(metadata, value));
    }

    public @NotNull SerializedEntity withData(@NotNull RNbtCompound data) {
        return new SerializedEntity(entityType, Objects.requireNonNull(data, "data"), passengers, originalUuid, serializedAt, metadata);
    }

    @Override
    public String toString() {
        return "SerializedEntity[entityType=" + entityTypeId() + ", dataKeys=" + data.keys() + ", passengers=" + passengers.size() + ", originalUuid=" + originalUuid + ']';
    }
}
