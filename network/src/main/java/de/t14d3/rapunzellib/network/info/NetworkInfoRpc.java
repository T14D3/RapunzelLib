package de.t14d3.rapunzellib.network.info;

import com.google.gson.reflect.TypeToken;
import de.t14d3.rapunzellib.network.runtime.RpcMethod;

import java.lang.reflect.Type;
import java.util.List;

public final class NetworkInfoRpc {
    private NetworkInfoRpc() {
    }

    public static final String SERVICE = "rapunzellib:netinfo";

    public static final String WHO_AM_I = "who_am_i";
    public static final String LIST_SERVERS = "list_servers";
    public static final String LIST_PLAYERS = "list_players";

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private static final Type PLAYER_LIST_TYPE = new TypeToken<List<NetworkPlayerInfo>>() {
    }.getType();

    public static final RpcMethod<Void, String> WHO_AM_I_METHOD = RpcMethod.of(SERVICE, WHO_AM_I, Void.class, String.class);
    public static final RpcMethod<Void, List<String>> LIST_SERVERS_METHOD = RpcMethod.of(
        SERVICE,
        LIST_SERVERS,
        Void.class,
        STRING_LIST_TYPE
    );
    public static final RpcMethod<Void, List<NetworkPlayerInfo>> LIST_PLAYERS_METHOD = RpcMethod.of(
        SERVICE,
        LIST_PLAYERS,
        Void.class,
        PLAYER_LIST_TYPE
    );
}
