package de.t14d3.rapunzellib.network.remote.rpc;

import de.t14d3.rapunzellib.network.runtime.RpcMethod;

public final class EntityServiceMethods {
    private EntityServiceMethods() {}

    public static final RpcMethod<Requests.EntityRef, Requests.LocationResult> GET_LOCATION =
        RpcMethod.of("entity", "getLocation", Requests.EntityRef.class, Requests.LocationResult.class);

    public static final RpcMethod<Requests.TeleportRequest, Requests.BooleanResult> TELEPORT =
        RpcMethod.of("entity", "teleport", Requests.TeleportRequest.class, Requests.BooleanResult.class);

    public static final RpcMethod<Requests.EntityRef, Requests.NameResult> GET_NAME =
        RpcMethod.of("entity", "getName", Requests.EntityRef.class, Requests.NameResult.class);

    public static final RpcMethod<Requests.SetNameRequest, Requests.VoidResult> SET_NAME =
        RpcMethod.of("entity", "setName", Requests.SetNameRequest.class, Requests.VoidResult.class);

    public static final RpcMethod<Requests.EntityRef, Requests.ComponentResult> GET_DISPLAY_NAME =
        RpcMethod.of("entity", "getDisplayName", Requests.EntityRef.class, Requests.ComponentResult.class);

    public static final RpcMethod<Requests.SetDisplayNameRequest, Requests.VoidResult> SET_DISPLAY_NAME =
        RpcMethod.of("entity", "setDisplayName", Requests.SetDisplayNameRequest.class, Requests.VoidResult.class);

    public static final RpcMethod<Requests.EntityRef, Requests.RemoveResult> REMOVE =
        RpcMethod.of("entity", "remove", Requests.EntityRef.class, Requests.RemoveResult.class);

    public static final RpcMethod<Requests.EntityRef, Requests.WorldRefResult> GET_WORLD =
        RpcMethod.of("entity", "getWorld", Requests.EntityRef.class, Requests.WorldRefResult.class);

    public static final RpcMethod<Requests.EntityPresenceRequest, Requests.EntityPresenceResult> QUERY_ENTITY_PRESENCE =
        RpcMethod.of("entity", "queryEntityPresence", Requests.EntityPresenceRequest.class, Requests.EntityPresenceResult.class);
}
