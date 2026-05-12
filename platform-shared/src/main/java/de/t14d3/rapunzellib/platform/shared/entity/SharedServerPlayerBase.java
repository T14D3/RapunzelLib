package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.inventory.Inventories;
import de.t14d3.rapunzellib.inventory.PlayerInventory;
import de.t14d3.rapunzellib.inventory.RInventory;
import de.t14d3.rapunzellib.nbt.shared.SharedAdventureComponentCodec;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Abstract base implementation of a server-connected player, wrapping a Minecraft {@link ServerPlayer}.
 * <p>
 * Provides shared implementations for player identity, location, health, inventory,
 * and custom name operations, delegating to {@link SharedEntityOperations} and
 * {@link SharedLivingEntitySupport} utility methods where appropriate.
 * </p>
 */
public abstract class SharedServerPlayerBase extends RNativeHandle<ServerPlayer> implements RServerPlayer, PlayerInventory {
    private final SharedWorldHooks worldHooks;

    /**
     * Constructs a new player base with a lazy-mutable attachment container and a world factory.
     *
     * @param platformId   the platform identifier
     * @param handle       the native Minecraft ServerPlayer
     * @param worldFactory a function to create {@link RWorld} wrappers from {@link ServerLevel} instances
     */
    protected SharedServerPlayerBase(
        @NotNull PlatformId platformId,
        @NotNull ServerPlayer handle,
        @NotNull Function<ServerLevel, ? extends RWorld> worldFactory
    ) {
        this(platformId, handle, RAttachmentContainer.lazyMutable(), SharedWorldHooks.of(worldFactory));
    }

    /**
     * Constructs a new player base with explicit attachments and world hooks.
     *
     * @param platformId   the platform identifier
     * @param handle       the native Minecraft ServerPlayer
     * @param attachments  the attachment container for this player
     * @param worldHooks   shared world creation and resolution hooks
     */
    protected SharedServerPlayerBase(
        @NotNull PlatformId platformId,
        @NotNull ServerPlayer handle,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, Objects.requireNonNull(handle, "handle"), Objects.requireNonNull(attachments, "attachments"));
        this.worldHooks = Objects.requireNonNull(worldHooks, "worldHooks");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull UUID uuid() {
        return handle().getUUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull String name() {
        return handle().getGameProfile().name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<RWorld> world() {
        ServerPlayer player = handle();
        ServerLevel level = (ServerLevel) player.level();
        return Optional.of(worldHooks.createWorld(level));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<RLocation> location() {
        ServerPlayer player = handle();
        ServerLevel level = (ServerLevel) player.level();
        return Optional.of(new RLocation(
            worldHooks.worldRef(level),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final double health() {
        return SharedLivingEntitySupport.health(handle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final double maxHealth() {
        return SharedLivingEntitySupport.maxHealth(handle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int remainingAir() {
        return SharedLivingEntitySupport.remainingAir(handle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int maxAir() {
        return SharedLivingEntitySupport.maxAir(handle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isAlive() {
        return SharedLivingEntitySupport.isAlive(handle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canTeleport() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean teleport(@NotNull RLocation location) {
        return SharedEntityOperations.teleport(handle(), location, worldHooks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canDamage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean damage(double amount) {
        return SharedEntityOperations.damage(handle(), amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canHeal() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean heal(double amount) {
        return SharedEntityOperations.heal(handle(), amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<String> getName() {
        Component customName = handle().getCustomName();
        if (customName == null) {
            return Optional.empty();
        }
        return Optional.of(PlainTextComponentSerializer.plainText().serialize(SharedAdventureComponentCodec.toAdventure(customName)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(@NotNull String name) {
        handle().setCustomName(Component.literal(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<net.kyori.adventure.text.Component> getDisplayName() {
        Component customName = handle().getCustomName();
        if (customName == null) {
            return Optional.empty();
        }
        return Optional.of(SharedAdventureComponentCodec.toAdventure(customName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisplayName(@NotNull net.kyori.adventure.text.Component displayName) {
        handle().setCustomName(SharedAdventureComponentCodec.toNative(displayName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove() {
        handle().remove(Entity.RemovalReason.DISCARDED);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemoved() {
        return handle().isRemoved();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull RInventory inventory() {
        return Rapunzel.context().services()
            .find(Inventories.class)
            .orElseThrow(() -> new UnsupportedOperationException(
                "Inventory feature not installed. Add a dependency on the inventory-<platform> module."))
            .require(handle().getInventory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull RInventory armor() {
        return Rapunzel.context().services()
            .find(Inventories.class)
            .orElseThrow(() -> new UnsupportedOperationException(
                "Inventory feature not installed. Add a dependency on the inventory-<platform> module."))
            .require(handle().getInventory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull RInventory enderChest() {
        return Rapunzel.context().services()
            .find(Inventories.class)
            .orElseThrow(() -> new UnsupportedOperationException(
                "Inventory feature not installed. Add a dependency on the inventory-<platform> module."))
            .require(handle().getEnderChestInventory());
    }
}
