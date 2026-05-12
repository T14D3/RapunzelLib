package de.t14d3.rapunzellib.network.rpc;

/**
 * Exception thrown when a remote procedure call fails.
 */
public final class RpcException extends RuntimeException {
    private final String requestId;
    private final String service;
    private final String method;
    private final String remote;

    /**
     * Creates a new RPC exception.
     *
     * @param requestId the request that failed
     * @param service the service name
     * @param method the method name
     * @param message the error message
     * @param remote the remote server description
     */
    public RpcException(String requestId, String service, String method, String message, String remote) {
        super(message);
        this.requestId = requestId;
        this.service = service;
        this.method = method;
        this.remote = remote;
    }

    /**
     * Returns the request ID.
     *
     * @return the request ID
     */
    public String requestId() {
        return requestId;
    }

    /**
     * Returns the service name.
     *
     * @return the service name
     */
    public String service() {
        return service;
    }

    /**
     * Returns the method name.
     *
     * @return the method name
     */
    public String method() {
        return method;
    }

    /**
     * Returns the remote server description.
     *
     * @return the remote server
     */
    public String remote() {
        return remote;
    }
}

