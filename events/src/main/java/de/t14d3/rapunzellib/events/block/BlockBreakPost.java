package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;

public record BlockBreakPost(RPlayer player, RBlock block, boolean cancelled) implements GamePostEvent {
}

