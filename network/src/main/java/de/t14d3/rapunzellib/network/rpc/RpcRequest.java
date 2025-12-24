package de.t14d3.rapunzellib.network.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public record RpcRequest(
    String requestId,
    String service,
    String method,
    JsonElement payload,
    long createdAt
) {
    public RpcRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId cannot be null/blank");
        }
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("service cannot be null/blank");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method cannot be null/blank");
        }
        if (payload == null) {
            payload = JsonNull.INSTANCE;
        }
    }
}

