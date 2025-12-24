package de.t14d3.rapunzellib.network.rpc;

public final class RpcException extends RuntimeException {
    private final String requestId;
    private final String service;
    private final String method;
    private final String remote;

    public RpcException(String requestId, String service, String method, String message, String remote) {
        super(message);
        this.requestId = requestId;
        this.service = service;
        this.method = method;
        this.remote = remote;
    }

    public String requestId() {
        return requestId;
    }

    public String service() {
        return service;
    }

    public String method() {
        return method;
    }

    public String remote() {
        return remote;
    }
}

