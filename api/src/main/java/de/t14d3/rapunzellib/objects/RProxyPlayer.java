package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A player connected through a proxy (e.g. Velocity, BungeeCord).
 *
 * <p>Proxy players may not have direct world or entity access, but provide
 * information about the backend server they are connected to.</p>
 */
public interface RProxyPlayer extends RPlayer {
    /**
     * Returns the name of the backend server this player is connected to, if available.
     *
     * @return an {@link Optional} containing the server name, or empty if unknown
     */
    @NotNull Optional<String> currentServerName();

    /**
     * Returns the name of the backend server this player is connected to, throwing if unavailable.
     *
     * @return the server name
     * @throws UnsupportedOperationException if the server name is not available
     */
    default @NotNull String currentServerNameOrThrow() {
        return currentServerName().orElseThrow(() -> new UnsupportedOperationException("currentServerName is not supported for " + getClass().getName()));
    }
}
