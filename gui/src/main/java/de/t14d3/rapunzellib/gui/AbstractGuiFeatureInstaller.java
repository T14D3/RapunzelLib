package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base implementation of {@link GuiFeatureInstaller} and {@link GuiRendererProvider}.
 * <p>
 * Provides a default installation flow that creates renderers, registers them in a
 * {@link GuiRendererRegistry}, and then registers a default renderer in the context.
 * Subclasses must override {@link #createRenderer(RapunzelContext)} or
 * {@link #provideRenderers(RapunzelContext)}.
 * </p>
 */
public abstract class AbstractGuiFeatureInstaller implements GuiFeatureInstaller, GuiRendererProvider {
    /**
     * Installs GUI features by providing renderers and registering the default one.
     *
     * @param context the Rapunzel context
     */
    @Override
    public final void install(@NotNull RapunzelContext context) {
        Collection<GuiRenderer> renderers = provideRenderers(context);
        Objects.requireNonNull(renderers, "provideRenderers(context)");

        GuiRendererRegistry registry = context.services().find(GuiRendererRegistry.class)
            .orElseGet(() -> {
                GuiRendererRegistry r = GuiRendererRegistry.create(context);
                context.register(GuiRendererRegistry.class, r);
                return r;
            });
        renderers.forEach(registry::registerRenderer);

        GuiRenderer defaultRenderer = resolveDefaultRenderer(renderers);
        beforeRegister(context, defaultRenderer);
        GuiFeatureInstallerSupport.registerGuiRenderer(context, defaultRenderer);
        afterRegister(context, defaultRenderer);
    }

    @Override
    public @NotNull Collection<GuiRenderer> provideRenderers(@NotNull RapunzelContext context) {
        return List.of(createRenderer(context));
    }

    protected void beforeRegister(@NotNull RapunzelContext context, @NotNull GuiRenderer renderer) {
    }

    protected void afterRegister(@NotNull RapunzelContext context, @NotNull GuiRenderer renderer) {
    }

    protected @NotNull GuiRenderer resolveDefaultRenderer(@NotNull Collection<GuiRenderer> renderers) {
        return renderers.stream()
            .filter(r -> r.name().contains("auto"))
            .findFirst()
            .orElse(renderers.iterator().next());
    }

    protected @NotNull GuiRenderer createRenderer(@NotNull RapunzelContext context) {
        throw new UnsupportedOperationException("Subclasses must override createRenderer or provideRenderers");
    }
}
