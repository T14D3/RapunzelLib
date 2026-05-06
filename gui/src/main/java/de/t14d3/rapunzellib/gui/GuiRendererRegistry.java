package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public interface GuiRendererRegistry {

    default void registerRenderer(@NotNull GuiRenderer renderer) {
        registerAllRenderers(List.of(renderer));
    }

    void registerAllRenderers(@NotNull Collection<@NotNull ? extends GuiRenderer> renderers);

    default void unregisterRenderer(@NotNull String name) {
        throw new UnsupportedOperationException("unregisterRenderer");
    }

    @NotNull Collection<@NotNull GuiRenderer> renderers();

    default @NotNull Optional<GuiRenderer> find(@NotNull String name) {
        return renderers().stream()
                .filter(r -> r.name().equals(name))
                .findFirst();
    }

    default @NotNull GuiRenderer get(@NotNull String name) {
        return find(name).orElseThrow(() ->
                new IllegalArgumentException("No renderer registered with name: " + name));
    }

    default boolean has(@NotNull String name) {
        return find(name).isPresent();
    }

    default @NotNull Optional<GuiRenderer> select(@NotNull Gui gui, @NotNull de.t14d3.rapunzellib.objects.RPlayer player) {
        return GuiRendererSelectionSupport.selectBest(renderers(), gui, player);
    }

    default @NotNull Collection<@NotNull GuiCapability> availableCapabilities() {
        return GuiRendererSelectionSupport.unionCapabilities(renderers());
    }

    static @NotNull GuiRendererRegistry create(@NotNull RapunzelContext context) {
        return new DefaultGuiRendererRegistry(context);
    }

    final class DefaultGuiRendererRegistry implements GuiRendererRegistry {
        private final RapunzelContext context;
        private final List<GuiRenderer> renderers = new CopyOnWriteArrayList<>();

        private DefaultGuiRendererRegistry(@NotNull RapunzelContext context) {
            this.context = context;
        }

        @Override
        public void registerAllRenderers(@NotNull Collection<@NotNull ? extends GuiRenderer> renderers) {
            for (GuiRenderer renderer : renderers) {
                if (renderer == null) {
                    throw new NullPointerException("Cannot register null renderer");
                }
                this.renderers.add(renderer);
            }
        }

        @Override
        public @NotNull Collection<@NotNull GuiRenderer> renderers() {
            return List.copyOf(renderers);
        }
    }
}
