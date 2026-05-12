package de.t14d3.rapunzellib.network.runtime;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Descriptor for an RPC method with request and response types.
 *
 * @param <Req> the request type
 * @param <Res> the response type
 */
public record RpcMethod<Req, Res>(
    @NotNull String service,
    @NotNull String method,
    @NotNull Type requestType,
    @NotNull Type responseType
) {
    public RpcMethod {
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("service cannot be blank");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method cannot be blank");
        }
        Objects.requireNonNull(requestType, "requestType");
        Objects.requireNonNull(responseType, "responseType");
    }

    public static <Req, Res> @NotNull RpcMethod<Req, Res> of(
        @NotNull String service,
        @NotNull String method,
        @NotNull Class<Req> requestType,
        @NotNull Class<Res> responseType
    ) {
        return new RpcMethod<>(service, method, requestType, responseType);
    }

    public static <Req, Res> @NotNull RpcMethod<Req, Res> of(
        @NotNull String service,
        @NotNull String method,
        @NotNull Type requestType,
        @NotNull Type responseType
    ) {
        return new RpcMethod<>(service, method, requestType, responseType);
    }

    public @NotNull String serviceMethod() {
        return service + "#" + method;
    }
}
