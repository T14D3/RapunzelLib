package de.t14d3.rapunzellib.gui.shared.dialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SharedDialogPayload(
    @NotNull String title,
    @NotNull List<Body> bodies,
    @NotNull List<Input> inputs
) {
    public SharedDialogPayload {
        bodies = List.copyOf(bodies);
        inputs = List.copyOf(inputs);
    }

    public sealed interface Body permits ButtonBody, TextBody, DividerBody, SpacerBody {
    }

    public record ButtonBody(
        @NotNull String label,
        boolean enabled,
        @Nullable String tooltip,
        @Nullable String icon
    ) implements Body {
    }

    public record TextBody(@NotNull String text) implements Body {
    }

    public record DividerBody() implements Body {
    }

    public record SpacerBody(int height) implements Body {
    }

    public sealed interface Input permits TextInput, ToggleInput, SliderInput, DropdownInput {
        @NotNull String key();

        @NotNull String label();
    }

    public record TextInput(
        @NotNull String key,
        @NotNull String label,
        @NotNull String placeholder,
        @Nullable String defaultValue,
        int maxLength
    ) implements Input {
    }

    public record ToggleInput(
        @NotNull String key,
        @NotNull String label,
        boolean defaultValue
    ) implements Input {
    }

    public record SliderInput(
        @NotNull String key,
        @NotNull String label,
        float min,
        float max,
        float step,
        float defaultValue
    ) implements Input {
    }

    public record DropdownInput(
        @NotNull String key,
        @NotNull String label,
        @NotNull List<DropdownOption> options,
        @Nullable String defaultValue
    ) implements Input {
        public DropdownInput {
            options = List.copyOf(options);
        }
    }

    public record DropdownOption(@NotNull String value, @NotNull String label) {
    }
}
