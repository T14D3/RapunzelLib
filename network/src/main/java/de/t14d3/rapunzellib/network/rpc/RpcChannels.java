package de.t14d3.rapunzellib.network.rpc;

import de.t14d3.rapunzellib.network.runtime.NetworkTopic;

public final class RpcChannels {
    private RpcChannels() {
    }

    public static final String REQUEST = "rapunzellib:rpc:req";
    public static final String RESPONSE = "rapunzellib:rpc:res";

    public static final NetworkTopic<RpcRequest> REQUEST_TOPIC = NetworkTopic.of(REQUEST, RpcRequest.class);
    public static final NetworkTopic<RpcResponse> RESPONSE_TOPIC = NetworkTopic.of(RESPONSE, RpcResponse.class);
}
