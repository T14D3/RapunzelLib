package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

class SpongeEntity extends RNativeHandle<Entity> implements REntity {
    private final SpongeWorlds worlds;

    SpongeEntity(Entity handle, @NotNull SpongeAttachmentService attachmentService, @NotNull SpongeWorlds worlds) {
        super(PlatformId.SPONGE, Objects.requireNonNull(handle, "handle"), Objects.requireNonNull(attachmentService, "attachmentService").forEntity(handle));
        this.worlds = Objects.requireNonNull(worlds, "worlds");
    }

    void updateHandle(Entity newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public @NotNull UUID uuid() {
        return handle().uniqueId();
    }

    @Override
    public @NotNull RRegistryRef<REntityType> typeRef() {
        return REntityType.ref(handle().type().key(RegistryTypes.ENTITY_TYPE).asString());
    }

    @Override
    public @NotNull Optional<RWorld> world() {
        return Optional.of(worlds.requireNative(handle().serverLocation().world()));
    }

    @Override
    public @NotNull Optional<RLocation> location() {
        return Optional.of(SpongeEntitySemantics.location(handle()));
    }

    @Override
    public boolean canTeleport() {
        return true;
    }

    @Override
    public boolean teleport(@NotNull RLocation location) {
        return SpongeEntitySemantics.teleport(handle(), location);
    }

    @Override
    public @NotNull Optional<String> getName() {
        return handle().get(Keys.CUSTOM_NAME).map(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c));
    }

    @Override
    public void setName(@NotNull String name) {
        handle().offer(Keys.CUSTOM_NAME, net.kyori.adventure.text.Component.text(name));
    }

    @Override
    public @NotNull Optional<net.kyori.adventure.text.Component> getDisplayName() {
        return handle().get(Keys.CUSTOM_NAME);
    }

    @Override
    public void setDisplayName(@NotNull net.kyori.adventure.text.Component displayName) {
        handle().offer(Keys.CUSTOM_NAME, displayName);
    }

    @Override
    public boolean remove() {
        handle().remove();
        return true;
    }

    @Override
    public boolean isRemoved() {
        return handle().isRemoved();
    }
}
