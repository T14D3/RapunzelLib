package de.t14d3.rapunzellib.network.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/**
 * A remote procedure call response.
 *
 * @param requestId the request this response corresponds to
 * @param ok whether the call succeeded
 * @param result the result value as JSON (if successful)
 * @param error error message (if failed)
 * @param createdAt timestamp when this response was created
 */
public record RpcResponse(
    String requestId,
    boolean ok,
    JsonElement result,
    String error,
    long createdAt
) {
    /**
     * Compact canonical constructor that validates and normalizes fields.
     */
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

