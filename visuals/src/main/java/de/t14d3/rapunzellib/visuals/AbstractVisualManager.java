package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractVisualManager implements VisualManager {
    protected final Map<VisualId, Visual<?>> visuals = new ConcurrentHashMap<>();

    @Override
    public void register(@NotNull Visual<?> visual) {
        visuals.put(visual.id(), visual);
    }

    @Override
    public void unregister(@NotNull Visual<?> visual) {
        visuals.remove(visual.id());
    }

    @Override
    public @NotNull Collection<Visual<?>> all() {
        return List.copyOf(visuals.values());
    }

    @Override
    public @NotNull Optional<Visual<?>> find(@NotNull VisualId id) {
        return Optional.ofNullable(visuals.get(id));
    }

    @Override
    public void removeAll() {
        for (Visual<?> v : new ArrayList<>(visuals.values())) {
            v.remove();
        }
        visuals.clear();
    }

    @Override
    public @NotNull BlockStructureVisual createBlockStructure(
        @NotNull BlockStructureConfig config,
        @NotNull VisualAudience audience
    ) {
        throw new UnsupportedOperationException("BlockStructureVisual not yet implemented on this platform");
    }

    @SuppressWarnings("unchecked")
    protected <C extends VisualConfig> Visual<C> castVisual(Visual<?> v) {
        return (Visual<C>) v;
    }

    @SuppressWarnings("unchecked")
    protected <V extends Visual<?>> List<V> castVisuals(Collection<Visual<?>> visuals) {
        return visuals.stream()
            .map(v -> (V) v)
            .toList();
    }

    public void cleanupForPlayer(@NotNull UUID uuid) {
        for (Visual<?> visual : visuals.values()) {
            if (visual instanceof AbstractVisual<?, ?> abstractVisual) {
                abstractVisual.onViewerQuit(uuid);
            }
        }
    }
}
