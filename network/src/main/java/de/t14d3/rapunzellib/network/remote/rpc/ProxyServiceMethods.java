package de.t14d3.rapunzellib.network.remote.rpc;

import de.t14d3.rapunzellib.network.runtime.RpcMethod;

public final class ProxyServiceMethods {
    private ProxyServiceMethods() {}

    public static final RpcMethod<Requests.ConnectToServerRequest, Requests.BooleanResult> PROXY_CONNECT_PLAYER =
        RpcMethod.of("proxy", "connectPlayer", Requests.ConnectToServerRequest.class, Requests.BooleanResult.class);
}
