package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public abstract class SharedServerPlayerBase extends RNativeHandle<ServerPlayer> implements RServerPlayer {
    private final SharedWorldHooks worldHooks;

    protected SharedServerPlayerBase(
        @NotNull PlatformId platformId,
        @NotNull ServerPlayer handle,
        @NotNull Function<ServerLevel, ? extends RWorld> worldFactory
    ) {
        this(platformId, handle, RAttachmentContainer.lazyMutable(), SharedWorldHooks.of(worldFactory));
    }

    protected SharedServerPlayerBase(
        @NotNull PlatformId platformId,
        @NotNull ServerPlayer handle,
        @NotNull RAttachmentContainer attachments,
        @NotNull SharedWorldHooks worldHooks
    ) {
        super(platformId, Objects.requireNonNull(handle, "handle"), Objects.requireNonNull(attachments, "attachments"));
        this.worldHooks = Objects.requireNonNull(worldHooks, "worldHooks");
    }

    @Override
    public final @NotNull UUID uuid() {
        return handle().getUUID();
    }

    @Override
    public final @NotNull String name() {
        return handle().getGameProfile().name();
    }

    @Override
    public final @NotNull Optional<RWorld> world() {
        ServerPlayer player = handle();
        ServerLevel level = (ServerLevel) player.level();
        return Optional.of(worldHooks.createWorld(level));
    }

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

    @Override
    public final double health() {
        return SharedLivingEntitySupport.health(handle());
    }

    @Override
    public final double maxHealth() {
        return SharedLivingEntitySupport.maxHealth(handle());
    }

    @Override
    public final int remainingAir() {
        return SharedLivingEntitySupport.remainingAir(handle());
    }

    @Override
    public final int maxAir() {
        return SharedLivingEntitySupport.maxAir(handle());
    }

    @Override
    public final boolean isAlive() {
        return SharedLivingEntitySupport.isAlive(handle());
    }

    @Override
    public boolean canTeleport() {
        return true;
    }

    @Override
    public boolean teleport(@NotNull RLocation location) {
        return SharedEntityOperations.teleport(handle(), location, worldHooks);
    }

    @Override
    public final boolean canDamage() {
        return true;
    }

    @Override
    public final boolean damage(double amount) {
        return SharedEntityOperations.damage(handle(), amount);
    }

    @Override
    public final boolean canHeal() {
        return true;
    }

    @Override
    public final boolean heal(double amount) {
        return SharedEntityOperations.heal(handle(), amount);
    }
}
