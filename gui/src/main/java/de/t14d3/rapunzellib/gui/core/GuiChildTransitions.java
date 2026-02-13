package de.t14d3.rapunzellib.gui.core;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiChildTransitions {
    private final Set<UUID> activeTransitions = ConcurrentHashMap.newKeySet();

    public boolean begin(@NotNull UUID playerId) {
        return activeTransitions.add(playerId);
    }

    public void end(@NotNull UUID playerId) {
        activeTransitions.remove(playerId);
    }

    public boolean contains(@NotNull UUID playerId) {
        return activeTransitions.contains(playerId);
    }
}
