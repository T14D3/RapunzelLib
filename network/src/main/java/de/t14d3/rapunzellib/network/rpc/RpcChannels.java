package de.t14d3.rapunzellib.network.rpc;

import de.t14d3.rapunzellib.network.runtime.NetworkTopic;

/**
 * Channel and topic constants for the RPC subsystem.
 *
 * <p>Defines the network channels used for RPC request/response message exchange.
 */
public final class RpcChannels {
    private RpcChannels() {
    }

    /** Channel name for RPC requests. */
    public static final String REQUEST = "rapunzellib:rpc:req";
    /** Channel name for RPC responses. */
    public static final String RESPONSE = "rapunzellib:rpc:res";

    /** Typed topic for RPC requests. */
    public static final NetworkTopic<RpcRequest> REQUEST_TOPIC = NetworkTopic.of(REQUEST, RpcRequest.class);
    /** Typed topic for RPC responses. */
    public static final NetworkTopic<RpcResponse> RESPONSE_TOPIC = NetworkTopic.of(RESPONSE, RpcResponse.class);
}
