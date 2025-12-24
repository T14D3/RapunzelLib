package de.t14d3.rapunzellib.network.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public record RpcResponse(
    String requestId,
    boolean ok,
    JsonElement result,
    String error,
    long createdAt
) {
    public RpcResponse {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId cannot be null/blank");
        }
        if (result == null) {
            result = JsonNull.INSTANCE;
        }
        if (!ok && error != null && error.isBlank()) {
            error = null;
        }
    }
}

