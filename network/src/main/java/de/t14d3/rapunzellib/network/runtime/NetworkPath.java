package de.t14d3.rapunzellib.network.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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

    public static @NotNull NetworkPath all() {
        return new NetworkPath(Target.ALL, null);
    }

    public static @NotNull NetworkPath proxy() {
        return new NetworkPath(Target.PROXY, null);
    }

    public static @NotNull NetworkPath server(@NotNull String serverName) {
        return new NetworkPath(Target.SERVER, serverName);
    }

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
