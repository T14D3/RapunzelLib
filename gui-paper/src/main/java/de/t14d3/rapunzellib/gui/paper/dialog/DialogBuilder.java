package de.t14d3.rapunzellib.gui.paper.dialog;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.context.ClickType;
import de.t14d3.rapunzellib.gui.core.GuiContexts;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogField;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValue;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValues;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModelBuilder;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogStateSupport;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogSubmissionProcessor;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.gui.element.ButtonElement;
import de.t14d3.rapunzellib.gui.element.DividerElement;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.gui.element.ItemElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.element.PaginationElement;
import de.t14d3.rapunzellib.gui.element.SliderElement;
import de.t14d3.rapunzellib.gui.element.SpacerElement;
import de.t14d3.rapunzellib.gui.element.TextElement;
import de.t14d3.rapunzellib.gui.element.ToggleElement;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.nbt.paper.PaperItemStackAdapter;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.ItemDialogBody;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DialogBuilder {

    private final GuiDialogModel model;
    private final RenderContext context;
    private final List<DialogBody> bodies = new ArrayList<>();
    private final List<DialogInput> inputs = new ArrayList<>();
    private final List<ActionButton> actionButtons = new ArrayList<>();

    public DialogBuilder(@NotNull Gui gui, @NotNull RenderContext context) {
        this.model = GuiDialogModelBuilder.build(gui, Component.text("Dialog"));
        this.context = context;
    }

    @Nullable
    public DialogBase build(@NotNull Component title) {
        for (GuiElement element : model.elements()) {
            processElement(element);
        }

        DialogBase.Builder builder = DialogBase.builder(title);
        if (!bodies.isEmpty()) {
            builder.body(bodies);
        }
        if (!inputs.isEmpty()) {
            builder.inputs(inputs);
        }
        return builder.build();
    }

    public boolean hasButtons() {
        return !actionButtons.isEmpty();
    }

    public List<ActionButton> getActionButtons() {
        return List.copyOf(actionButtons);
    }

    private void processElement(@NotNull GuiElement element) {
        switch (element) {
            case ButtonElement button -> processButton(button);
            case TextElement text -> bodies.add(DialogBody.plainMessage(text.text()));
            case InputElement input -> inputs.add(createInput((GuiDialogField.InputField) model.keyedFields().get(input.key())));
            case ToggleElement toggle -> inputs.add(createToggle((GuiDialogField.ToggleField) model.keyedFields().get(toggle.key())));
            case SliderElement slider -> inputs.add(createSlider((GuiDialogField.SliderField) model.keyedFields().get(slider.key())));
            case DropdownElement dropdown -> inputs.add(createDropdown((GuiDialogField.DropdownField) model.keyedFields().get(dropdown.key())));
            case ItemElement item -> processItem(item);
            case PaginationElement pagination -> bodies.add(DialogBody.plainMessage(
                Component.text("Page " + (pagination.currentPage() + 1) + " of " + pagination.totalPages())
            ));
            case DividerElement dividerIgnored -> {
            }
            case SpacerElement spacerIgnored -> {
            }
            default -> {
            }
        }
    }

    private void processButton(@NotNull ButtonElement button) {
        if (button.onClick() != null) {
            actionButtons.add(
                ActionButton.builder(button.label())
                    .action(DialogAction.customClick((response, audience) -> handleButtonClick(button, response), ClickCallback.Options.builder().build()))
                    .build()
            );
            return;
        }
        bodies.add(DialogBody.plainMessage(button.label()));
    }

    private void handleButtonClick(@NotNull ButtonElement button, @NotNull DialogResponseView response) {
        GuiDialogSubmissionProcessor.submit(model, collectValues(response), context.player(), context.state());
        button.onClick().accept(GuiContexts.click(context.player(), button, -1, ClickType.LEFT, context.state()));
    }

    private @NotNull GuiDialogFieldValues collectValues(@NotNull DialogResponseView response) {
        return GuiDialogStateSupport.collectSubmittedValues(model, field -> switch (field) {
            case GuiDialogField.InputField ignored -> GuiValue.of(response.getText(field.key()));
            case GuiDialogField.ToggleField ignored -> GuiValue.of(response.getBoolean(field.key()));
            case GuiDialogField.SliderField ignored -> GuiValue.of(response.getFloat(field.key()));
            case GuiDialogField.DropdownField ignored -> GuiValue.of(response.getText(field.key()));
            default -> null;
        });
    }

    private @NotNull TextDialogInput createInput(@NotNull GuiDialogField.InputField input) {
        TextDialogInput.Builder builder = DialogInput.text(
            input.key(),
            input.labelOrKey()
        );
        GuiDialogFieldValue.TextValue currentValue = (GuiDialogFieldValue.TextValue) GuiDialogStateSupport.currentValue(input, context.state());
        String stringValue = currentValue.value();
        if (!stringValue.isEmpty()) {
            builder.initial(stringValue);
        }
        if (input.element().maxLength() > 0) {
            builder.maxLength(input.element().maxLength());
        }
        return builder.build();
    }

    private @NotNull BooleanDialogInput createToggle(@NotNull GuiDialogField.ToggleField toggle) {
        GuiDialogFieldValue.ToggleValue currentValue = (GuiDialogFieldValue.ToggleValue) GuiDialogStateSupport.currentValue(toggle, context.state());
        return DialogInput.bool(
            toggle.key(),
            toggle.labelOrKey(),
            currentValue.value(),
            "true",
            "false"
        );
    }

    private @NotNull NumberRangeDialogInput createSlider(@NotNull GuiDialogField.SliderField slider) {
        GuiDialogFieldValue.SliderValue currentValue = (GuiDialogFieldValue.SliderValue) GuiDialogStateSupport.currentValue(slider, context.state());
        return DialogInput.numberRange(
            slider.key(),
            200,
            slider.labelOrKey(),
            slider.element().format(),
            slider.element().min(),
            slider.element().max(),
            currentValue.value(),
            slider.element().step() > 0 ? slider.element().step() : null
        );
    }

    private @NotNull SingleOptionDialogInput createDropdown(@NotNull GuiDialogField.DropdownField dropdown) {
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        GuiDialogFieldValue.DropdownValue currentValue = (GuiDialogFieldValue.DropdownValue) GuiDialogStateSupport.currentValue(dropdown, context.state());
        String defaultId = currentValue.selectedId();
        for (Option option : dropdown.element().options()) {
            entries.add(SingleOptionDialogInput.OptionEntry.create(option.id(), option.display(), option.id().equals(defaultId)));
        }
        return DialogInput.singleOption(
            dropdown.key(),
            dropdown.labelOrKey(),
            entries
        ).build();
    }

    private void processItem(@NotNull ItemElement item) {
        ItemStack stack = adapter().create(item.item());
        ItemDialogBody.Builder builder = DialogBody.item(stack);
        bodies.add(builder.build());
    }

    private static @NotNull PaperItemStackAdapter adapter() {
        return (PaperItemStackAdapter) NbtFeatures.itemStackAdapter(ItemStack.class);
    }
}
