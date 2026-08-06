package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.inventory.Inventories;
import de.t14d3.rapunzellib.inventory.PlayerInventory;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.objects.RGameMode;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class SpongePlayer extends RNativeHandle<ServerPlayer> implements RServerPlayer, PlayerInventory {
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

    @Override
    public @NotNull RInventory inventory() {
        return inventories().require(handle().inventory());
    }

    @Override
    public @NotNull RInventory armor() {
        return inventories().require(handle().inventory().armor());
    }

    @Override
    public @NotNull RInventory enderChest() {
        return inventories().require(handle().enderChestInventory());
    }

    @Override
    public void gameMode(@NotNull RGameMode gameMode) {
        handle().offer(Keys.GAME_MODE, toSpongeGameMode(gameMode));
    }

    @Override
    public @NotNull RGameMode gameMode() {
        return toRapunzelGameMode(handle().gameMode().get());
    }

    @Override
    public void op(boolean op) {
        // Sponge models the vanilla operator status as the "minecraft.command"
        // permission on the player's subject data.
        handle().subjectData().setPermission(
            Set.of(),
            "minecraft.command",
            op ? Tristate.TRUE : Tristate.FALSE
        ).join();
    }

    @Override
    public boolean op() {
        return handle().hasPermission("minecraft.command");
    }

    private @NotNull Inventories inventories() {
        return de.t14d3.rapunzellib.Rapunzel.context().services()
            .find(Inventories.class)
            .orElseThrow(() -> new UnsupportedOperationException(
                "Inventory feature not installed. Add a dependency on the inventory-sponge module."));
    }

    private static @NotNull GameMode toSpongeGameMode(@NotNull RGameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> GameModes.SURVIVAL.get();
            case CREATIVE -> GameModes.CREATIVE.get();
            case ADVENTURE -> GameModes.ADVENTURE.get();
            case SPECTATOR -> GameModes.SPECTATOR.get();
        };
    }

    private static @NotNull RGameMode toRapunzelGameMode(@NotNull GameMode gameMode) {
        if (GameModes.SURVIVAL.get().equals(gameMode)) return RGameMode.SURVIVAL;
        if (GameModes.CREATIVE.get().equals(gameMode)) return RGameMode.CREATIVE;
        if (GameModes.ADVENTURE.get().equals(gameMode)) return RGameMode.ADVENTURE;
        if (GameModes.SPECTATOR.get().equals(gameMode)) return RGameMode.SPECTATOR;
        return RGameMode.SURVIVAL;
    }
}
