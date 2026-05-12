package de.t14d3.rapunzellib.network.info;

import java.util.UUID;

/**
 * Information about a player on the network.
 *
 * @param uuid the player's UUID
 * @param name the player's name
 * @param serverName the server the player is connected to
 */
public record NetworkPlayerInfo(
    UUID uuid,
    String name,
    String serverName
) {
}

