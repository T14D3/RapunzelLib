package de.t14d3.rapunzellib.network.info;

import java.util.UUID;

public record NetworkPlayerInfo(
    UUID uuid,
    String name,
    String serverName
) {
}

