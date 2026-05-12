package de.t14d3.rapunzellib.network.info;

import com.google.gson.reflect.TypeToken;
import de.t14d3.rapunzellib.network.runtime.RpcMethod;

import java.lang.reflect.Type;
import java.util.List;

/**
 * RPC method constants for the network info service.
 *
 * <p>Defines the service name, method names, and typed RPC method descriptors
 * for querying network information from the proxy.
 */
public final class NetworkInfoRpc {
    private NetworkInfoRpc() {
    }

    /** RPC service name for network info. */
    public static final String SERVICE = "rapunzellib:netinfo";

    /** RPC method to query the proxy for the backend's network server name. */
    public static final String WHO_AM_I = "who_am_i";
    /** RPC method to list all registered servers. */
    public static final String LIST_SERVERS = "list_servers";
    /** RPC method to list all online players. */
    public static final String LIST_PLAYERS = "list_players";

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private static final Type PLAYER_LIST_TYPE = new TypeToken<List<NetworkPlayerInfo>>() {
    }.getType();

    /** RPC method descriptor for querying the network server name. */
    public static final RpcMethod<Void, String> WHO_AM_I_METHOD = RpcMethod.of(SERVICE, WHO_AM_I, Void.class, String.class);
    /** RPC method descriptor for listing servers. */
    public static final RpcMethod<Void, List<String>> LIST_SERVERS_METHOD = RpcMethod.of(
        SERVICE,
        LIST_SERVERS,
        Void.class,
        STRING_LIST_TYPE
    );
    /** RPC method descriptor for listing players. */
    public static final RpcMethod<Void, List<NetworkPlayerInfo>> LIST_PLAYERS_METHOD = RpcMethod.of(
        SERVICE,
        LIST_PLAYERS,
        Void.class,
        PLAYER_LIST_TYPE
    );
}
