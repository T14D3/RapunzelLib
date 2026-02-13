package de.t14d3.rapunzellib.gui.core;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogField;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValue;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValues;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModelBuilder;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogStateSupport;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogSubmissionProcessor;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.element.SliderElement;
import de.t14d3.rapunzellib.gui.element.ToggleElement;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiSharedCoreTest {

    private static final RPlayer PLAYER = new TestPlayer();

    @Test
    void interactionEngineMutatesStateAndDispatchesCallbacks() {
        GuiState state = GuiState.create();
        AtomicReference<Boolean> toggleValue = new AtomicReference<>();
        AtomicReference<Float> sliderValue = new AtomicReference<>();
        AtomicReference<String> inputValue = new AtomicReference<>();
        AtomicReference<String> dropdownValue = new AtomicReference<>();
        AtomicReference<GuiValue> dropdownData = new AtomicReference<>();

        ToggleElement toggle = ToggleElement.builder()
            .key("toggle")
            .defaultValue(false)
            .onChange(ctx -> toggleValue.set(ctx.value()))
            .build();
        SliderElement slider = SliderElement.builder()
            .key("slider")
            .min(0.0f)
            .max(10.0f)
            .step(2.0f)
            .defaultValue(4.0f)
            .onChange(ctx -> sliderValue.set(ctx.value()))
            .build();
        InputElement input = InputElement.builder()
            .key("input")
            .maxLength(4)
            .onChange(ctx -> inputValue.set(ctx.value()))
            .build();
        DropdownElement dropdown = DropdownElement.builder()
            .key("dropdown")
            .defaultValue(Option.of("a", "A"))
            .option("a", Component.text("A"))
            .option(Option.of("b", Component.text("B"), 42.0d))
            .onChange(ctx -> {
                dropdownValue.set(ctx.selectedId());
                dropdownData.set(ctx.selectedOption() != null ? ctx.selectedOption().data() : null);
            })
            .build();

        assertTrue(GuiInteractionEngine.toggle(toggle, state, PLAYER));
        assertEquals(Boolean.TRUE, state.get("toggle", Boolean.class));
        assertEquals(Boolean.TRUE, toggleValue.get());

        assertEquals(6.0f, GuiInteractionEngine.slide(slider, state, PLAYER, 2.0f));
        assertEquals(6.0f, state.get("slider", Float.class));
        assertEquals(6.0f, sliderValue.get());

        assertEquals("tool", GuiInteractionEngine.submitInput(input, state, PLAYER, "toolong"));
        assertEquals("tool", state.get("input", String.class));
        assertEquals("tool", inputValue.get());

        GuiInteractionEngine.DropdownSelection selection = GuiInteractionEngine.selectDropdown(dropdown, state, PLAYER, "b");
        assertEquals("b", selection.selectedId());
        assertEquals("b", state.get("dropdown", String.class));
        assertEquals("b", dropdownValue.get());
        assertEquals("b", selection.selectedOption().id());
        assertEquals(42.0d, selection.selectedOption().data().doubleValue(0.0d));
        assertEquals(42.0d, dropdownData.get().doubleValue(0.0d));
    }

    @Test
    void dialogModelBuilderAndSubmissionProcessorShareInteractiveMetadata() {
        AtomicReference<String> inputValue = new AtomicReference<>();

        Gui gui = Gui.builder()
            .title("Dialog")
            .text(text -> text.text(Component.text("Intro")))
            .input(input -> input.key("name").label(Component.text("Name")).onChange(ctx -> inputValue.set(ctx.value())))
            .toggle(toggle -> toggle.key("enabled").defaultValue(true))
            .button(button -> button.label(Component.text("Save")))
            .build();

        GuiDialogModel model = GuiDialogModelBuilder.build(gui, Component.text("Fallback"));

        assertEquals(Component.text("Dialog"), model.title());
        assertEquals(4, model.elements().size());
        assertEquals(2, model.interactiveFields().size());
        assertEquals(1, model.buttons().size());
        assertSame(model.keyedFields().get("name"), model.interactiveFields().get(0));
        GuiDialogField field = model.keyedFields().get("name");
        assertTrue(field instanceof GuiDialogField.InputField);
        assertEquals("", ((GuiDialogFieldValue.TextValue) field.currentValue(GuiState.create())).value());

        GuiState state = GuiState.create();
        GuiDialogSubmissionProcessor.submit(
            model,
            Map.of("name", GuiValue.of("Alex"), "enabled", GuiValue.of(false)),
            PLAYER,
            state
        );

        assertEquals("Alex", state.get("name", String.class));
        assertEquals(Boolean.FALSE, state.get("enabled", Boolean.class));
        assertEquals("Alex", inputValue.get());
        assertNull(model.keyedFields().get("missing"));
    }

    @Test
    void dialogSubmissionRetainsDropdownOptionData() {
        Gui gui = Gui.builder()
            .title("Dialog")
            .dropdown(dropdown -> dropdown.key("choice")
                .option(Option.of("a", Component.text("A"), "alpha"))
                .option(Option.of("b", Component.text("B"), true)))
            .build();

        GuiDialogModel model = GuiDialogModelBuilder.build(gui, Component.text("Fallback"));
        GuiDialogFieldValues values = GuiDialogStateSupport.collectSubmittedValues(
            model,
            field -> field.key().equals("choice") ? GuiValue.of("b") : null
        );

        GuiDialogFieldValue.DropdownValue dropdownValue = (GuiDialogFieldValue.DropdownValue) values.value("choice");
        assertEquals("b", dropdownValue.selectedId());
        assertEquals(Boolean.TRUE, dropdownValue.selectedData().booleanValue());
    }

    private static final class TestPlayer implements RPlayer {
        private final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");

        @Override
        public @NotNull UUID uuid() {
            return uuid;
        }

        @Override
        public @NotNull String name() {
            return "tester";
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return true;
        }

        @Override
        public Audience audience() {
            return Audience.empty();
        }

        @Override
        public @NotNull PlatformId platformId() {
            return PlatformId.PAPER;
        }

        @Override
        public @NotNull Object handle() {
            return this;
        }
    }
}
