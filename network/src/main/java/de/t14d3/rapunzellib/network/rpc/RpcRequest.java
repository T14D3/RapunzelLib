package de.t14d3.rapunzellib.network.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/**
 * A remote procedure call request.
 *
 * @param requestId unique identifier for this request
 * @param service the target service name
 * @param method the method to invoke
 * @param payload the method arguments as JSON
 * @param createdAt timestamp when this request was created
 */
public record RpcRequest(
    String requestId,
    String service,
    String method,
    JsonElement payload,
    long createdAt
) {
    /**
     * Compact canonical constructor that validates required fields.
     */
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

