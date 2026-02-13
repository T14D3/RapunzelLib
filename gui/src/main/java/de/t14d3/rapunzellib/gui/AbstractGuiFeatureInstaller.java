package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractGuiFeatureInstaller implements GuiFeatureInstaller {
    @Override
    public final void install(@NotNull RapunzelContext context) {
        GuiRenderer renderer = createRenderer(context);
        beforeRegister(context, renderer);
        GuiFeatureInstallerSupport.registerGuiRenderer(context, renderer);
        afterRegister(context, renderer);
    }

    protected void beforeRegister(@NotNull RapunzelContext context, @NotNull GuiRenderer renderer) {
    }

    protected void afterRegister(@NotNull RapunzelContext context, @NotNull GuiRenderer renderer) {
    }

    protected abstract @NotNull GuiRenderer createRenderer(@NotNull RapunzelContext context);
}
