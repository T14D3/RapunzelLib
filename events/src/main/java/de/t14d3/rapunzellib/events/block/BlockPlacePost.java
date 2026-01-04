package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

public record BlockPlacePost(
    RPlayer player,
    RWorldRef world,
    RBlockPos pos,
    String blockTypeKey,
    boolean cancelled
) implements GamePostEvent {
}
