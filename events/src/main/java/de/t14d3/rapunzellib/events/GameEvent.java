package de.t14d3.rapunzellib.events;

public sealed interface GameEvent permits GamePreEvent, GamePostEvent, GameEventSnapshot {
}

