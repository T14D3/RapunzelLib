package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

public interface Visuals {
    @NotNull VisualManager manager();

    @NotNull
    default VisualBuilder builder() {
        return new VisualBuilder(manager());
    }
}
