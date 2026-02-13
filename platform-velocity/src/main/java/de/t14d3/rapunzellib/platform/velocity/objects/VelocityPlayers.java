package de.t14d3.rapunzellib.platform.velocity.objects;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.common.objects.AbstractPlayerStore;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class VelocityPlayers extends AbstractPlayerStore<Player, VelocityPlayer> {
    private final ProxyServer proxy;
    private final VelocityPersistentAttachmentsStore persistentAttachmentsStore;

    public VelocityPlayers(@NotNull ProxyServer proxy, @NotNull VelocityPersistentAttachmentsStore persistentAttachmentsStore) {
        this.proxy = Objects.requireNonNull(proxy, "proxy");
        this.persistentAttachmentsStore = Objects.requireNonNull(persistentAttachmentsStore, "persistentAttachmentsStore");
    }

    @Override
    protected @NotNull Collection<? extends Player> nativeOnlinePlayers() {
        return proxy.getAllPlayers();
    }

    @Override
    protected @NotNull Optional<? extends Player> findNativePlayer(@NotNull UUID uuid) {
        return proxy.getPlayer(uuid);
    }

    @Override
    protected @NotNull Optional<? extends Player> adaptNativePlayer(@NotNull Object nativePlayer) {
        return nativePlayer instanceof Player player ? Optional.of(player) : Optional.empty();
    }

    @Override
    protected @NotNull UUID playerId(@NotNull Player nativePlayer) {
        return nativePlayer.getUniqueId();
    }

    @Override
    protected @NotNull VelocityPlayer createWrapper(@NotNull Player nativeHandle) {
        return new VelocityPlayer(nativeHandle, persistentAttachmentsStore);
    }

    @Override
    protected void updateWrapper(@NotNull VelocityPlayer existingWrapper, @NotNull Player nativeHandle) {
        existingWrapper.updateHandle(nativeHandle);
    }
}
