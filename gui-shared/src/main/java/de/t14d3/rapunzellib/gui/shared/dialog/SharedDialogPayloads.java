package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogStateSupport;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogField;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValue;
import de.t14d3.rapunzellib.gui.core.GuiInventoryPresentation;
import de.t14d3.rapunzellib.gui.element.ButtonElement;
import de.t14d3.rapunzellib.gui.element.DividerElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.Icon;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.element.PaginationElement;
import de.t14d3.rapunzellib.gui.element.SpacerElement;
import de.t14d3.rapunzellib.gui.element.TextElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link SharedDialogPayload} instances from
 * {@link GuiDialogModel} and render context.
 */
public final class SharedDialogPayloads {
    private SharedDialogPayloads() {
    }

    /**
     * Creates a shared dialog payload from a dialog model and context.
     *
     * @param model   the dialog model
     * @param context the render context
     * @return the shared dialog payload
     */
    public static @NotNull SharedDialogPayload create(
        @NotNull GuiDialogModel model,
        @NotNull de.t14d3.rapunzellib.gui.RenderContext context
    ) {
        return new SharedDialogPayload(
            plain(model.title()),
            createBodies(model),
            createInputs(model, context)
        );
    }

    /**
     * Creates the body elements list from the model.
     *
     * @param model the dialog model
     * @return the list of body elements
     */
    private static @NotNull List<SharedDialogPayload.Body> createBodies(@NotNull GuiDialogModel model) {
        List<SharedDialogPayload.Body> bodies = new ArrayList<>();
        for (GuiElement element : model.elements()) {
            SharedDialogPayload.Body entry = switch (element) {
                case ButtonElement button -> createButton(button);
                case TextElement text -> new SharedDialogPayload.TextBody(plain(text.text()));
                case DividerElement ignored -> new SharedDialogPayload.DividerBody();
                case SpacerElement spacer -> new SharedDialogPayload.SpacerBody(spacer.height());
                case PaginationElement pagination -> new SharedDialogPayload.TextBody(
                    "Page " + (pagination.currentPage() + 1) + " of " + pagination.totalPages()
                );
                default -> null;
            };
            if (entry != null) {
                bodies.add(entry);
            }
        }
        return bodies;
    }

    /**
     * Creates a button body from a button element.
     *
     * @param button the button element
     * @return the button body
     */
    private static @NotNull SharedDialogPayload.ButtonBody createButton(@NotNull ButtonElement button) {
        return new SharedDialogPayload.ButtonBody(
            plain(button.label()),
            button.enabled(),
            plain(button.tooltip()),
            iconId(button.icon())
        );
    }

    /**
     * Creates the input fields list from the model.
     *
     * @param model   the dialog model
     * @param context the render context
     * @return the list of input fields
     */
    private static @NotNull List<SharedDialogPayload.Input> createInputs(
        @NotNull GuiDialogModel model,
        @NotNull de.t14d3.rapunzellib.gui.RenderContext context
    ) {
        List<SharedDialogPayload.Input> inputs = new ArrayList<>();
        for (GuiDialogField field : model.interactiveFields()) {
            SharedDialogPayload.Input entry = switch (field) {
                case GuiDialogField.InputField input -> createInput(input, context);
                case GuiDialogField.ToggleField toggle -> createToggle(toggle, context);
                case GuiDialogField.SliderField slider -> createSlider(slider, context);
                case GuiDialogField.DropdownField dropdown -> createDropdown(dropdown, context);
                default -> null;
            };
            if (entry != null) {
                inputs.add(entry);
            }
        }
        return inputs;
    }

    /**
     * Creates a text input payload from an input field.
     *
     * @param input   the input field
     * @param context the render context
     * @return the text input payload
     */
    private static @NotNull SharedDialogPayload.TextInput createInput(
        @NotNull GuiDialogField.InputField input,
        @NotNull de.t14d3.rapunzellib.gui.RenderContext context
    ) {
        GuiDialogFieldValue.TextValue currentValue = (GuiDialogFieldValue.TextValue) GuiDialogStateSupport.currentValue(input, context.state());
        return new SharedDialogPayload.TextInput(
            input.key(),
            labelOrKey(input.key(), input.label()),
            input.element().placeholder() != null ? input.element().placeholder() : "",
            currentValue.value(),
            input.element().maxLength()
        );
    }

    /**
     * Creates a toggle input payload from a toggle field.
     *
     * @param toggle  the toggle field
     * @param context the render context
     * @return the toggle input payload
     */
    private static @NotNull SharedDialogPayload.ToggleInput createToggle(
        @NotNull GuiDialogField.ToggleField toggle,
        @NotNull de.t14d3.rapunzellib.gui.RenderContext context
    ) {
        GuiDialogFieldValue.ToggleValue currentValue = (GuiDialogFieldValue.ToggleValue) GuiDialogStateSupport.currentValue(toggle, context.state());
        return new SharedDialogPayload.ToggleInput(
            toggle.key(),
            labelOrKey(toggle.key(), toggle.label()),
            currentValue.value()
        );
    }

    /**
     * Creates a slider input payload from a slider field.
     *
     * @param slider  the slider field
     * @param context the render context
     * @return the slider input payload
     */
    private static @NotNull SharedDialogPayload.SliderInput createSlider(
        @NotNull GuiDialogField.SliderField slider,
        @NotNull de.t14d3.rapunzellib.gui.RenderContext context
    ) {
        GuiDialogFieldValue.SliderValue currentValue = (GuiDialogFieldValue.SliderValue) GuiDialogStateSupport.currentValue(slider, context.state());
        return new SharedDialogPayload.SliderInput(
            slider.key(),
            labelOrKey(slider.key(), slider.label()),
            slider.element().min(),
            slider.element().max(),
            slider.element().step(),
            currentValue.value()
        );
    }

    /**
     * Creates a dropdown input payload from a dropdown field.
     *
     * @param dropdown the dropdown field
     * @param context  the render context
     * @return the dropdown input payload
     */
    private static @NotNull SharedDialogPayload.DropdownInput createDropdown(
        @NotNull GuiDialogField.DropdownField dropdown,
        @NotNull de.t14d3.rapunzellib.gui.RenderContext context
    ) {
        List<SharedDialogPayload.DropdownOption> options = new ArrayList<>();
        for (Option option : dropdown.element().options()) {
            options.add(new SharedDialogPayload.DropdownOption(option.id(), plain(option.display())));
        }
        GuiDialogFieldValue.DropdownValue currentValue = (GuiDialogFieldValue.DropdownValue) GuiDialogStateSupport.currentValue(dropdown, context.state());

        return new SharedDialogPayload.DropdownInput(
            dropdown.key(),
            labelOrKey(dropdown.key(), dropdown.label()),
            options,
            currentValue.selectedId()
        );
    }

    /**
     * Extracts the icon identifier from an Icon.
     *
     * @param icon the icon
     * @return the icon id, or {@code null}
     */
    private static @Nullable String iconId(@Nullable Icon icon) {
        return switch (icon) {
            case Icon.ItemIcon itemIcon -> GuiInventoryPresentation.normalizeItemKey(itemIcon.item().material(), "minecraft:stone");
            case Icon.CustomIcon customIcon -> customIcon.id();
            case Icon.NoneIcon ignored -> null;
            case null -> null;
        };
    }

    /**
     * Returns the label or falls back to the key if label is null.
     *
     * @param key   the field key
     * @param label the component label
     * @return the plain text label
     */
    private static @NotNull String labelOrKey(@NotNull String key, Component label) {
        return label != null ? plain(label) : key;
    }

    /**
     * Converts a component to plain text.
     *
     * @param component the component
     * @return the plain text
     */
    private static @NotNull String plain(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Converts an array of components to plain text strings.
     *
     * @param components the component array
     * @return the plain text array
     */
    private static @NotNull String[] plain(@NotNull Component[] components) {
        String[] strings = new String[components.length];
        for (int i = 0; i < components.length; i++) {
            strings[i] = plain(components[i]);
        }
        return strings;
    }

}
