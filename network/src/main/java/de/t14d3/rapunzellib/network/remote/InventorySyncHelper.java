package de.t14d3.rapunzellib.network.remote;

import de.t14d3.rapunzellib.network.remote.proxy.RemotePlayer;
import de.t14d3.rapunzellib.network.remote.rpc.Requests;
import de.t14d3.rapunzellib.objects.RServerPlayer;
import de.t14d3.rapunzellib.objects.snapshot.InventorySnapshot;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class InventorySyncHelper {
    private InventorySyncHelper() {}

    public static @NotNull CompletableFuture<Optional<InventorySnapshot>> fetchRemote(
        @NotNull RServerPlayer player, @NotNull String inventoryType) {
        Objects.requireNonNull(player, "player");
        if (player instanceof RemotePlayer remote) {
            return remote.fetchInventoryAsync()
                .thenApply(result -> {
                    if (!result.found()) return Optional.empty();
                    var slots = result.slots().stream()
                        .map(s -> new InventorySnapshot.SlotEntry(s.slot(), s.itemNbt()))
                        .toList();
                    return Optional.of(InventorySnapshot.of(result.size(), slots, inventoryType));
                });
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    public static @NotNull CompletableFuture<Boolean> applyRemote(
        @NotNull RServerPlayer player, @NotNull InventorySnapshot snapshot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        if (player instanceof RemotePlayer remote) {
            var slots = snapshot.slots().stream()
                .map(s -> new Requests.SlotEntry(s.slot(), s.itemNbt()))
                .toList();
            return remote.setInventoryAsync(slots, snapshot.inventoryType());
        }
        return CompletableFuture.completedFuture(false);
    }

    public static @NotNull CompletableFuture<Boolean> syncToRemote(@NotNull RServerPlayer source,
                                                                     @NotNull RServerPlayer destination) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        if (source instanceof RemotePlayer remoteSource) {
            return remoteSource.fetchInventoryAsync()
                .thenCompose(result -> {
                    if (!result.found()) return CompletableFuture.completedFuture(false);
                    if (destination instanceof RemotePlayer remoteDest) {
                        return remoteDest.setInventoryAsync(result.slots(), result.inventoryType());
                    }
                    return CompletableFuture.completedFuture(false);
                });
        }
        return CompletableFuture.completedFuture(false);
    }
}
