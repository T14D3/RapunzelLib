package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class SpongePlayer extends RNativeHandle<ServerPlayer> implements RServerPlayer {
    private final SpongeWorlds worlds;

    SpongePlayer(ServerPlayer handle, @NotNull SpongeAttachmentService attachmentService, @NotNull SpongeWorlds worlds) {
        super(PlatformId.SPONGE, Objects.requireNonNull(handle, "handle"), Objects.requireNonNull(attachmentService, "attachmentService").forPlayer(handle));
        this.worlds = Objects.requireNonNull(worlds, "worlds");
    }

    void updateHandle(ServerPlayer newHandle) {
        updateNativeHandle(Objects.requireNonNull(newHandle, "newHandle"));
    }

    @Override
    public @NotNull Audience audience() {
        return handle();
    }

    @Override
    public @NotNull UUID uuid() {
        return handle().profile().uuid();
    }

    @Override
    public @NotNull String name() {
        return handle().profile().name().orElseGet(() -> handle().profile().uuid().toString());
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return permission == null || permission.isBlank() || handle().hasPermission(permission);
    }

    @Override
    public @NotNull Optional<RWorld> world() {
        return Optional.of(worlds.requireNative(handle().world()));
    }

    @Override
    public @NotNull Optional<RLocation> location() {
        return Optional.of(SpongeEntitySemantics.location(handle()));
    }

    @Override
    public double health() {
        return handle().health().get();
    }

    @Override
    public double maxHealth() {
        return handle().maxHealth().get();
    }

    @Override
    public int remainingAir() {
        return handle().requireValue(Keys.REMAINING_AIR).get();
    }

    @Override
    public int maxAir() {
        return handle().requireValue(Keys.MAX_AIR).get();
    }

    @Override
    public boolean isAlive() {
        return !handle().isRemoved() && handle().health().get() > 0.0d;
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
    public boolean canDamage() {
        return true;
    }

    @Override
    public boolean damage(double amount) {
        return SpongeEntitySemantics.damage(handle(), amount);
    }

    @Override
    public boolean canHeal() {
        return true;
    }

    @Override
    public boolean heal(double amount) {
        return SpongeEntitySemantics.heal(handle(), amount);
    }

    @Override
    public @NotNull Optional<String> getName() {
        return handle().get(org.spongepowered.api.data.Keys.CUSTOM_NAME).map(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c));
    }

    @Override
    public void setName(@NotNull String name) {
        handle().offer(org.spongepowered.api.data.Keys.CUSTOM_NAME, Component.text(name));
    }

    @Override
    public @NotNull Optional<Component> getDisplayName() {
        return handle().get(org.spongepowered.api.data.Keys.CUSTOM_NAME);
    }

    @Override
    public void setDisplayName(@NotNull Component displayName) {
        handle().offer(org.spongepowered.api.data.Keys.CUSTOM_NAME, displayName);
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
