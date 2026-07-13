package de.t14d3.rapunzellib.objects.snapshot;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable async-safe entity snapshot captured from a live entity wrapper.
 */
public record REntitySnapshot(
    @NotNull UUID uuid,
    @NotNull RWorldRef world,
    @NotNull RBlockPos pos,
    @NotNull RRegistryRef<REntityType> typeRef,
    @Nullable RLocation preciseLocation
) {
    public REntitySnapshot {
        uuid = Objects.requireNonNull(uuid, "uuid");
        world = Objects.requireNonNull(world, "world");
        pos = Objects.requireNonNull(pos, "pos");
        typeRef = Objects.requireNonNull(typeRef, "typeRef");
    }

    /** Returns the entity type key from the type reference. */
    public @NotNull RKey entityTypeKey() {
        return typeRef.key();
    }

    /** Returns the entity type ID string. */
    public @NotNull String entityTypeId() {
        return entityTypeKey().asString();
    }

    /** Returns the precise location, if captured. */
    public @NotNull Optional<RLocation> location() {
        return Optional.ofNullable(preciseLocation);
    }

    /** Returns the precise location, throwing if not captured. */
    public @NotNull RLocation requireLocation() {
        return location().orElseThrow(() -> new IllegalStateException("Entity snapshot does not include precise location"));
    }

    /** Captures a snapshot of the given live entity's current state. */
    public static @NotNull REntitySnapshot capture(@NotNull REntity entity) {
        Objects.requireNonNull(entity, "entity");
        RLocation location = entity.location().orElse(null);
        RWorldRef world = entity.worldRef().orElseThrow(() -> new IllegalArgumentException(
            "Entity does not expose a live world reference: " + entity.getClass().getName()
        ));
        RBlockPos pos = location != null
            ? location.blockPos()
            : entity.blockPos().orElseThrow(() -> new IllegalArgumentException(
                "Entity does not expose a live block position: " + entity.getClass().getName()
            ));
        return new REntitySnapshot(entity.uuid(), world, pos, entity.typeRef(), location);
    }

    /** Creates a snapshot from explicit values without a precise location. */
    public static @NotNull REntitySnapshot of(
        @NotNull UUID uuid,
        @NotNull RWorldRef world,
        @NotNull RBlockPos pos,
        @NotNull RKey entityTypeKey
    ) {
        return new REntitySnapshot(uuid, world, pos, REntityType.ref(entityTypeKey), null);
    }

    public static @NotNull REntitySnapshot of(
        @NotNull UUID uuid,
        @NotNull RWorldRef world,
        @NotNull RBlockPos pos,
        @NotNull String entityTypeKey
    ) {
        return of(uuid, world, pos, RKey.of(entityTypeKey));
    }
}
