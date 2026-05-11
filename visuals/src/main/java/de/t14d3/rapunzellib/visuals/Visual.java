package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

public interface Visual<C extends VisualConfig> {
    @NotNull VisualId id();

    @NotNull C config();

    @NotNull VisualAudience audience();

    void show();

    void hide();

    void remove();

    boolean isShown();
}
