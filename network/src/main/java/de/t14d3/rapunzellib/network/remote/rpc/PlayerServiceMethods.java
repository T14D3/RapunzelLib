package de.t14d3.rapunzellib.network.remote.rpc;

import de.t14d3.rapunzellib.network.runtime.RpcMethod;

public final class PlayerServiceMethods {
    private PlayerServiceMethods() {}

    public static final RpcMethod<Requests.SendMessageRequest, Requests.VoidResult> SEND_MESSAGE =
        RpcMethod.of("player", "sendMessage", Requests.SendMessageRequest.class, Requests.VoidResult.class);

    public static final RpcMethod<Requests.SendMessageRequest, Requests.VoidResult> SEND_ACTION_BAR =
        RpcMethod.of("player", "sendActionBar", Requests.SendMessageRequest.class, Requests.VoidResult.class);

    public static final RpcMethod<Requests.DamageRequest, Requests.BooleanResult> DAMAGE =
        RpcMethod.of("player", "damage", Requests.DamageRequest.class, Requests.BooleanResult.class);

    public static final RpcMethod<Requests.HealRequest, Requests.BooleanResult> HEAL =
        RpcMethod.of("player", "heal", Requests.HealRequest.class, Requests.BooleanResult.class);

    public static final RpcMethod<Requests.PlayerRef, Requests.HealthResult> GET_HEALTH =
        RpcMethod.of("player", "getHealth", Requests.PlayerRef.class, Requests.HealthResult.class);

    public static final RpcMethod<Requests.PlayerRef, Requests.AirResult> GET_AIR =
        RpcMethod.of("player", "getAir", Requests.PlayerRef.class, Requests.AirResult.class);

    public static final RpcMethod<Requests.PlayerRef, Requests.AliveResult> IS_ALIVE =
        RpcMethod.of("player", "isAlive", Requests.PlayerRef.class, Requests.AliveResult.class);

    public static final RpcMethod<Requests.PermissionRequest, Requests.PermissionResult> HAS_PERMISSION =
        RpcMethod.of("player", "hasPermission", Requests.PermissionRequest.class, Requests.PermissionResult.class);

    public static final RpcMethod<Requests.ConnectToServerRequest, Requests.BooleanResult> CONNECT_TO_SERVER =
        RpcMethod.of("player", "connectToServer", Requests.ConnectToServerRequest.class, Requests.BooleanResult.class);

    public static final RpcMethod<Requests.InventorySnapshotRequest, Requests.InventorySnapshotResult> GET_INVENTORY =
        RpcMethod.of("player", "getInventory", Requests.InventorySnapshotRequest.class, Requests.InventorySnapshotResult.class);

    public static final RpcMethod<Requests.SetInventoryRequest, Requests.BooleanResult> SET_INVENTORY =
        RpcMethod.of("player", "setInventory", Requests.SetInventoryRequest.class, Requests.BooleanResult.class);

    public static final RpcMethod<Requests.ServerPlayersRequest, Requests.ServerPlayersResult> QUERY_SERVER_PLAYERS =
        RpcMethod.of("player", "queryServerPlayers", Requests.ServerPlayersRequest.class, Requests.ServerPlayersResult.class);
}