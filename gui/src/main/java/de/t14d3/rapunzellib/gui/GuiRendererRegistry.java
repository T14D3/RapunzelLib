package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry managing available {@link GuiRenderer} instances.
 * <p>
 * Allows registration, lookup, and selection of renderers for a given GUI and player.
 * </p>
 */
public interface GuiRendererRegistry {

    /**
     * Registers a single renderer.
     *
     * @param renderer the renderer to register
     */
    default void registerRenderer(@NotNull GuiRenderer renderer) {
        registerAllRenderers(List.of(renderer));
    }

    /**
     * Registers all given renderers.
     *
     * @param renderers the renderers to register
     */
    void registerAllRenderers(@NotNull Collection<@NotNull ? extends GuiRenderer> renderers);

    /**
     * Unregisters a renderer by name.
     *
     * @param name the name of the renderer to unregister
     */
    default void unregisterRenderer(@NotNull String name) {
        throw new UnsupportedOperationException("unregisterRenderer");
    }

    /**
     * Returns all registered renderers.
     *
     * @return an immutable collection of renderers
     */
    @NotNull Collection<@NotNull GuiRenderer> renderers();

    /**
     * Finds a renderer by name.
     *
     * @param name the renderer name
     * @return an Optional containing the renderer, or empty if not found
     */
    default @NotNull Optional<GuiRenderer> find(@NotNull String name) {
        return renderers().stream()
                .filter(r -> r.name().equals(name))
                .findFirst();
    }

    /**
     * Gets a renderer by name, throwing if not found.
     *
     * @param name the renderer name
     * @return the renderer
     * @throws IllegalArgumentException if no renderer is registered with that name
     */
    default @NotNull GuiRenderer get(@NotNull String name) {
        return find(name).orElseThrow(() ->
                new IllegalArgumentException("No renderer registered with name: " + name));
    }

    /**
     * Checks whether a renderer with the given name is registered.
     *
     * @param name the renderer name
     * @return true if registered
     */
    default boolean has(@NotNull String name) {
        return find(name).isPresent();
    }

    /**
     * Selects the best renderer for the given GUI and player.
     *
     * @param gui    the GUI to render
     * @param player the target player
     * @return an Optional containing the best renderer, or empty if none available
     */
    default @NotNull Optional<GuiRenderer> select(@NotNull Gui gui, @NotNull de.t14d3.rapunzellib.objects.RPlayer player) {
        return GuiRendererSelectionSupport.selectBest(renderers(), gui, player);
    }

    /**
     * Returns the union of all capabilities across all registered renderers.
     *
     * @return an immutable collection of capabilities
     */
    default @NotNull Collection<@NotNull GuiCapability> availableCapabilities() {
        return GuiRendererSelectionSupport.unionCapabilities(renderers());
    }

    /**
     * Creates a new registry for the given context.
     *
     * @param context the Rapunzel context
     * @return a new registry instance
     */
    static @NotNull GuiRendererRegistry create(@NotNull RapunzelContext context) {
        return new DefaultGuiRendererRegistry(context);
    }

    /**
     * Default implementation of {@link GuiRendererRegistry}.
     */
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
