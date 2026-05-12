package de.t14d3.rapunzellib.gui.shared.dialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the serializable payload of a dialog GUI, including its title,
 * body elements, and interactive input fields.
 */
public record SharedDialogPayload(
    @NotNull String title,
    @NotNull List<Body> bodies,
    @NotNull List<Input> inputs
) {
    /**
     * Creates a payload with defensive copies of the lists.
     *
     * @param title  the dialog title
     * @param bodies the body elements
     * @param inputs the input fields
     */
    public SharedDialogPayload {
        bodies = List.copyOf(bodies);
        inputs = List.copyOf(inputs);
    }

    /**
     * A sealed interface for dialog body elements.
     */
    public sealed interface Body permits ButtonBody, TextBody, DividerBody, SpacerBody {
    }

    /**
     * A clickable button in the dialog body.
     *
     * @param label   the button label
     * @param enabled whether the button is enabled
     * @param tooltip optional tooltip lines
     * @param icon    optional icon identifier
     */
    public record ButtonBody(
        @NotNull String label,
        boolean enabled,
        @Nullable String[] tooltip,
        @Nullable String icon
    ) implements Body {
    }

    /**
     * A text element in the dialog body.
     *
     * @param text the text content
     */
    public record TextBody(@NotNull String text) implements Body {
    }

    /**
     * A visual divider in the dialog body.
     */
    public record DividerBody() implements Body {
    }

    /**
     * A spacer element with configurable height.
     *
     * @param height the spacer height
     */
    public record SpacerBody(int height) implements Body {
    }

    /**
     * A sealed interface for interactive input fields.
     */
    public sealed interface Input permits TextInput, ToggleInput, SliderInput, DropdownInput {
        /**
         * Returns the key identifying this input.
         *
         * @return the input key
         */
        @NotNull String key();

        /**
         * Returns the label for this input.
         *
         * @return the label
         */
        @NotNull String label();
    }

    /**
     * A text input field.
     *
     * @param key          the field key
     * @param label        the field label
     * @param placeholder  the placeholder text
     * @param defaultValue the default value
     * @param maxLength    the maximum input length
     */
    public record TextInput(
        @NotNull String key,
        @NotNull String label,
        @NotNull String placeholder,
        @Nullable String defaultValue,
        int maxLength
    ) implements Input {
    }

    /**
     * A toggle/boolean input field.
     *
     * @param key          the field key
     * @param label        the field label
     * @param defaultValue the default value
     */
    public record ToggleInput(
        @NotNull String key,
        @NotNull String label,
        boolean defaultValue
    ) implements Input {
    }

    /**
     * A slider input field with a numeric range.
     *
     * @param key          the field key
     * @param label        the field label
     * @param min          the minimum value
     * @param max          the maximum value
     * @param step         the step increment
     * @param defaultValue the default value
     */
    public record SliderInput(
        @NotNull String key,
        @NotNull String label,
        float min,
        float max,
        float step,
        float defaultValue
    ) implements Input {
    }

    /**
     * A dropdown/select input field.
     *
     * @param key          the field key
     * @param label        the field label
     * @param options      the selectable options
     * @param defaultValue the default selected value
     */
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

    /**
     * An option within a dropdown input.
     *
     * @param value the internal value
     * @param label the display label
     */
    public record DropdownOption(@NotNull String value, @NotNull String label) {
    }
}
