package de.t14d3.rapunzellib.gui.element;

import de.t14d3.rapunzellib.gui.value.GuiValue;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Option(@NotNull String id, @NotNull Component display, @Nullable GuiValue data) {
    @NotNull
    public static Option of(@NotNull String id, @NotNull Component display) {
        return new Option(id, display, null);
    }
    
    @NotNull
    public static Option of(@NotNull String id, @NotNull Component display, @Nullable GuiValue data) {
        return new Option(id, display, data);
    }

    @NotNull
    public static Option of(@NotNull String id, @NotNull Component display, @NotNull String data) {
        return new Option(id, display, GuiValue.of(data));
    }

    @NotNull
    public static Option of(@NotNull String id, @NotNull Component display, boolean data) {
        return new Option(id, display, GuiValue.of(data));
    }

    @NotNull
    public static Option of(@NotNull String id, @NotNull Component display, double data) {
        return new Option(id, display, GuiValue.of(data));
    }
    
    @NotNull
    public static Option of(@NotNull String id, @NotNull String display) {
        return new Option(id, Component.text(display), null);
    }
}
