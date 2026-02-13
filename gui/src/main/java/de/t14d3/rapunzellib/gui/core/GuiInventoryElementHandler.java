package de.t14d3.rapunzellib.gui.core;

import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.gui.context.ClickType;
import de.t14d3.rapunzellib.gui.element.ButtonElement;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.gui.element.ItemElement;
import de.t14d3.rapunzellib.gui.element.SliderElement;
import de.t14d3.rapunzellib.gui.element.ToggleElement;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public final class GuiInventoryElementHandler {
    private GuiInventoryElementHandler() {
    }

    public static @NotNull Result handle(
        @NotNull GuiElement element,
        @NotNull ClickContext clickContext,
        @NotNull Consumer<InputElement> inputOpener,
        @NotNull Consumer<DropdownElement> dropdownOpener
    ) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(clickContext, "clickContext");
        Objects.requireNonNull(inputOpener, "inputOpener");
        Objects.requireNonNull(dropdownOpener, "dropdownOpener");

        return switch (element) {
            case ButtonElement button -> {
                if (button.onClick() != null) {
                    button.onClick().accept(clickContext);
                }
                yield Result.none();
            }
            case ItemElement item -> {
                if (item.onClick() != null) {
                    item.onClick().accept(clickContext);
                }
                yield Result.none();
            }
            case ToggleElement toggle -> Result.stateMutation(
                GuiInteractionEngine.toggle(toggle, clickContext.state(), clickContext.player())
            );
            case SliderElement slider -> Result.stateMutation(
                GuiInteractionEngine.slide(slider, clickContext.state(), clickContext.player(), sliderDelta(slider, clickContext.clickType()))
            );
            case InputElement input -> {
                inputOpener.accept(input);
                yield Result.child(ChildType.INPUT);
            }
            case DropdownElement dropdown -> {
                dropdownOpener.accept(dropdown);
                yield Result.child(ChildType.DROPDOWN);
            }
            default -> Result.none();
        };
    }

    public static float sliderDelta(@NotNull SliderElement slider, @NotNull ClickType clickType) {
        float step = slider.step() > 0.0f ? slider.step() : 1.0f;
        return switch (clickType) {
            case RIGHT, SHIFT_RIGHT -> -step;
            default -> step;
        };
    }

    public enum ChildType {
        INPUT,
        DROPDOWN,
    }

    public record Result(boolean stateMutated, @Nullable ChildType childType, @Nullable GuiValue value) {
        public static @NotNull Result none() {
            return new Result(false, null, null);
        }

        public static @NotNull Result stateMutation(boolean value) {
            return new Result(true, null, GuiValue.of(value));
        }

        public static @NotNull Result stateMutation(float value) {
            return new Result(true, null, GuiValue.of(value));
        }

        public static @NotNull Result child(@NotNull ChildType childType) {
            return new Result(false, childType, null);
        }

        public boolean openedChild() {
            return !stateMutated && childType != null;
        }
    }
}
