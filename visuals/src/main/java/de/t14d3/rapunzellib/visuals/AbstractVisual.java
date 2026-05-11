package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractVisual<C extends VisualConfig, P> implements Visual<C> {
    protected final VisualId id;
    protected final C config;
    protected final VisualAudience audience;
    protected volatile boolean shown = false;
    protected final Set<UUID> currentViewers = ConcurrentHashMap.newKeySet();
    private final VisualManager manager;

    protected AbstractVisual(
        @NotNull VisualId id,
        @NotNull C config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager
    ) {
        this.id = id;
        this.config = config;
        this.audience = audience;
        this.manager = manager;
    }

    @Override
    public final @NotNull VisualId id() {
        return id;
    }

    @Override
    public final @NotNull C config() {
        return config;
    }

    @Override
    public final @NotNull VisualAudience audience() {
        return audience;
    }

    @Override
    public final boolean isShown() {
        return shown;
    }

    protected synchronized void showInternal() {
        if (shown) return;
        shown = true;
        currentViewers.clear();
    }

    protected synchronized void hideInternal() {
        if (!shown) return;
        shown = false;
        currentViewers.clear();
    }

    @Override
    public final void remove() {
        hideInternal();
        manager.unregister(this);
    }

    public final void onViewerQuit(@NotNull UUID uuid) {
        currentViewers.remove(uuid);
    }

    public final Set<UUID> currentViewers() {
        return Collections.unmodifiableSet(currentViewers);
    }

    protected final VisualManager manager() {
        return manager;
    }
}
