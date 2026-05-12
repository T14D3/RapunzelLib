package de.t14d3.rapunzellib.network.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A routing path describing the target and optional server name for a message.
 *
 * @param target     the routing target (ALL, PROXY, or SERVER)
 * @param serverName the specific server name when target is SERVER
 */
public record NetworkPath(@NotNull Target target, @Nullable String serverName) {
    public NetworkPath {
        Objects.requireNonNull(target, "target");
        if (target == Target.SERVER) {
            if (serverName == null || serverName.isBlank()) {
                throw new IllegalArgumentException("serverName cannot be blank for SERVER paths");
            }
        } else {
            serverName = null;
        }
    }

    /**
     * Creates a path targeting all servers.
     *
     * @return the ALL path
     */
    public static @NotNull NetworkPath all() {
        return new NetworkPath(Target.ALL, null);
    }

    /**
     * Creates a path targeting the proxy server.
     *
     * @return the PROXY path
     */
    public static @NotNull NetworkPath proxy() {
        return new NetworkPath(Target.PROXY, null);
    }

    /**
     * Creates a path targeting a specific server.
     *
     * @param serverName the target server name
     * @return the SERVER path
     */
    public static @NotNull NetworkPath server(@NotNull String serverName) {
        return new NetworkPath(Target.SERVER, serverName);
    }

    /**
     * Returns a human-readable description of this path.
     *
     * @return the path description
     */
    public @NotNull String describe() {
        return switch (target) {
            case ALL -> "all";
            case PROXY -> "proxy";
            case SERVER -> "server:" + serverName;
        };
    }

    public enum Target {
        ALL,
        PROXY,
        SERVER
    }
}
