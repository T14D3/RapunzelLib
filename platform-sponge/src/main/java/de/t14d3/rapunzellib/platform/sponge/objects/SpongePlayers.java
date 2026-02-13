package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.common.objects.AbstractPlayerStore;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class SpongePlayers extends AbstractPlayerStore<ServerPlayer, SpongePlayer> {
    private final SpongeAttachmentService attachmentService;
    private final SpongeWorlds worlds;

    public SpongePlayers(@NotNull SpongeAttachmentService attachmentService, @NotNull SpongeWorlds worlds) {
        this.attachmentService = attachmentService;
        this.worlds = worlds;
    }

    @Override
    protected @NotNull Collection<? extends ServerPlayer> nativeOnlinePlayers() {
        if (!Sponge.isServerAvailable()) {
            return java.util.List.of();
        }
        return Sponge.server().onlinePlayers();
    }

    @Override
    protected @NotNull Optional<? extends ServerPlayer> findNativePlayer(@NotNull UUID uuid) {
        if (!Sponge.isServerAvailable()) {
            return Optional.empty();
        }
        return Sponge.server().player(uuid);
    }

    @Override
    protected @NotNull Optional<? extends ServerPlayer> adaptNativePlayer(@NotNull Object nativePlayer) {
        return nativePlayer instanceof ServerPlayer player ? Optional.of(player) : Optional.empty();
    }

    @Override
    protected @NotNull UUID playerId(@NotNull ServerPlayer nativePlayer) {
        return nativePlayer.profile().uuid();
    }

    @Override
    protected @NotNull SpongePlayer createWrapper(@NotNull ServerPlayer nativeHandle) {
        return new SpongePlayer(nativeHandle, attachmentService, worlds);
    }

    @Override
    protected void updateWrapper(@NotNull SpongePlayer existingWrapper, @NotNull ServerPlayer nativeHandle) {
        existingWrapper.updateHandle(nativeHandle);
    }
}
