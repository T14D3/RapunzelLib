package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation of the {@link Visual} interface.
 * <p>
 * Manages common visual state: identity, configuration, audience,
 * visibility flag, and the set of current viewers.
 *
 * @param <C> the visual configuration type
 * @param <P> the platform-specific packet/handle type
 */
public abstract class AbstractVisual<C extends VisualConfig, P> implements Visual<C> {

    /** The unique visual identifier. */
    protected final VisualId id;

    /** The visual configuration. */
    protected final C config;

    /** The audience this visual targets. */
    protected final VisualAudience audience;

    /** Whether the visual is currently shown. */
    protected volatile boolean shown = false;

    /** The set of player UUIDs currently viewing this visual. */
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

    /**
     * Internal method to mark the visual as shown.
     * Clears the current viewer set to prepare for fresh tracking.
     */
    protected synchronized void showInternal() {
        if (shown) return;
        shown = true;
        currentViewers.clear();
    }

    /**
     * Internal method to mark the visual as hidden.
     * Clears the current viewer set.
     */
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

    /**
     * Notifies this visual that a player has quit, removing them from the viewer set.
     *
     * @param uuid the UUID of the player that quit
     */
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
