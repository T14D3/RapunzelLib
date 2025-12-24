package de.t14d3.rapunzellib.objects;

import java.util.Optional;

/**
 * @deprecated Use {@link RPlayer} and {@link RPlayer#currentServerName()} instead.
 */
@Deprecated(forRemoval = true, since = "0.1.2")
public interface RProxyPlayer extends RPlayer {
    @Override
    Optional<String> currentServerName();
}

