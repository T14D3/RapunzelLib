package de.t14d3.rapunzellib.network.remote.rpc;

import com.google.gson.JsonElement;
import de.t14d3.rapunzellib.network.runtime.RpcMethod;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class Requests {
    private Requests() {}

    public record PlayerRef(@NotNull UUID uuid) {}
    public record EntityRef(@NotNull UUID uuid) {}

    public record TeleportRequest(@NotNull UUID uuid, @NotNull RWorldRef world,
                                   double x, double y, double z,
                                   float yaw, float pitch) {}

    public record BooleanResult(boolean success) {}
    public record VoidResult() {}

    public record DoubleResult(double value) {}
    public record DoubleDoubleResult(double value1, double value2) {}
    public record IntResult(int value) {}
    public record StringResult(@Nullable String value) {}
    public record ComponentResult(@Nullable JsonElement componentJson) {}

    public record LocationResult(@Nullable RWorldRef world,
                                  double x, double y, double z,
                                  float yaw, float pitch,
                                  boolean found) {}

    public record NameResult(@Nullable String name) {}

    public record SetNameRequest(@NotNull UUID uuid, @Nullable String name) {}

    public record SetDisplayNameRequest(@NotNull UUID uuid, @Nullable JsonElement componentJson) {}

    public record SendMessageRequest(@NotNull UUID uuid, @NotNull JsonElement componentJson) {}

    public record PermissionRequest(@NotNull UUID uuid, @NotNull String permission) {}
    public record PermissionResult(boolean hasPermission) {}

    public record DamageRequest(@NotNull UUID uuid, double amount) {}
    public record HealRequest(@NotNull UUID uuid, double amount) {}

    public record HealthResult(double health, double maxHealth) {}
    public record AirResult(int remainingAir, int maxAir) {}
    public record AliveResult(boolean alive) {}

    public record WorldRefResult(@Nullable RWorldRef worldRef, boolean found) {}
    public record RemoveResult(boolean removed) {}

    public record ConnectToServerRequest(@NotNull UUID uuid, @NotNull String targetServer,
                                          @Nullable RWorldRef world,
                                          double x, double y, double z,
                                          float yaw, float pitch,
                                          boolean hasLocation) {}

    public record SlotEntry(int slot, @Nullable String itemNbt) {}
    public record InventorySnapshotRequest(@NotNull UUID uuid) {}
    public record InventorySnapshotResult(@NotNull String inventoryType,
                                           int size,
                                           @NotNull List<SlotEntry> slots,
                                           boolean found) {}
    public record SetInventoryRequest(@NotNull UUID uuid,
                                       @NotNull String inventoryType,
                                       @NotNull List<SlotEntry> slots) {}
}
