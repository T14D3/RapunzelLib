package de.t14d3.rapunzellib.gui.sponge.dialog;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogField;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValue;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModel;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogModelBuilder;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogStateSupport;
import de.t14d3.rapunzellib.gui.element.*;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

final class DialogBuilder {

    private static final String DIALOG_CLASS = "org.spongepowered.api.ui.dialog.Dialog";
    private static final String INPUT_CLASS = "org.spongepowered.api.ui.dialog.input.DialogInput";

    @Nullable
    Object build(@NotNull Gui gui, @NotNull RenderContext context) {
        return build(GuiDialogModelBuilder.build(gui, Component.empty()), context);
    }

    @Nullable
    Object build(@NotNull GuiDialogModel model, @NotNull RenderContext context) {
        List<DialogComponent> components = new ArrayList<>();

        for (GuiElement element : model.elements()) {
            DialogComponent component = createComponent(element, model, context);
            if (component != null) {
                components.add(component);
            }
        }

        return createNativeDialog(model.title(), components);
    }

    @Nullable
    private DialogComponent createComponent(@NotNull GuiElement element, @NotNull GuiDialogModel model, @NotNull RenderContext context) {
        return switch (element.type()) {
            case TEXT -> createTextComponent((TextElement) element);
            case INPUT -> createInputComponent((GuiDialogField.InputField) model.keyedFields().get(((InputElement) element).key()), context);
            case TOGGLE -> createToggleComponent((GuiDialogField.ToggleField) model.keyedFields().get(((ToggleElement) element).key()), context);
            case SLIDER -> createSliderComponent((GuiDialogField.SliderField) model.keyedFields().get(((SliderElement) element).key()), context);
            case DROPDOWN -> createDropdownComponent((GuiDialogField.DropdownField) model.keyedFields().get(((DropdownElement) element).key()), context);
            case BUTTON -> createButtonComponent((ButtonElement) element);
            case DIVIDER -> createDividerComponent();
            default -> null;
        };
    }

    @NotNull
    private DialogComponent createTextComponent(@NotNull TextElement text) {
        return new DialogComponent("text", text.text(), null);
    }

    @NotNull
    private DialogComponent createInputComponent(@NotNull GuiDialogField.InputField input, @NotNull RenderContext context) {
        GuiDialogFieldValue.TextValue currentValue = (GuiDialogFieldValue.TextValue) GuiDialogStateSupport.currentValue(input, context.state());
        String value = currentValue.value();
        return new DialogComponent("input", input.label(), value);
    }

    @NotNull
    private DialogComponent createToggleComponent(@NotNull GuiDialogField.ToggleField toggle, @NotNull RenderContext context) {
        boolean value = ((GuiDialogFieldValue.ToggleValue) GuiDialogStateSupport.currentValue(toggle, context.state())).value();
        return new DialogComponent("toggle", toggle.label(), value);
    }

    @NotNull
    private DialogComponent createSliderComponent(@NotNull GuiDialogField.SliderField slider, @NotNull RenderContext context) {
        float value = ((GuiDialogFieldValue.SliderValue) GuiDialogStateSupport.currentValue(slider, context.state())).value();
        return new DialogComponent("slider", slider.label(), Map.of(
            "value", value,
            "min", slider.element().min(),
            "max", slider.element().max(),
            "step", slider.element().step()
        ));
    }

    @NotNull
    private DialogComponent createDropdownComponent(@NotNull GuiDialogField.DropdownField dropdown, @NotNull RenderContext context) {
        List<String> options = new ArrayList<>();
        for (Option option : dropdown.element().options()) {
            options.add(option.id());
        }
        GuiDialogFieldValue.DropdownValue currentValue = (GuiDialogFieldValue.DropdownValue) GuiDialogStateSupport.currentValue(dropdown, context.state());
        String selected = currentValue.selectedId();
        return new DialogComponent("dropdown", dropdown.label(), Map.of(
            "options", options,
            "selected", selected
        ));
    }

    @NotNull
    private DialogComponent createButtonComponent(@NotNull ButtonElement button) {
        return new DialogComponent("button", button.label(), null);
    }

    @NotNull
    private DialogComponent createDividerComponent() {
        return new DialogComponent("divider", Component.empty(), null);
    }

    @Nullable
    private Object createNativeDialog(@NotNull Component title, @NotNull List<DialogComponent> components) {
        final Class<?> dialogClass;
        try {
            dialogClass = Class.forName(DIALOG_CLASS);
        } catch (ClassNotFoundException e) {
            return null;
        }

        Object dialog = createViaBuilder(dialogClass, title, components);
        if (dialog != null) {
            return dialog;
        }

        dialog = createViaStaticFactory(dialogClass, title, components);
        if (dialog != null) {
            return dialog;
        }

        return createViaConstructor(dialogClass, title, components);
    }

    @Nullable
    private Object createViaBuilder(@NotNull Class<?> dialogClass, @NotNull Component title, @NotNull List<DialogComponent> components) {
        Object builder = invokeStaticNoArg(dialogClass, "builder", "createBuilder", "newBuilder", "builderOf");
        if (builder == null) {
            return null;
        }

        invokeInstanceWithSingleArgument(builder, title, "title", "name", "label", "header", "caption");
        invokeInstanceWithSingleArgument(builder, toPayload(components), "components", "content", "elements", "inputs", "fields");

        for (DialogComponent component : components) {
            Object nativeComponent = createNativeComponent(component);
            Object payload = nativeComponent != null ? nativeComponent : toPayload(component);
            if (!invokeInstanceWithSingleArgument(builder, payload, "component", "addComponent", "add", "element", "addElement", "input", "addInput", "field", "addField")) {
                break;
            }
        }

        return invokeInstanceNoArg(builder, "build", "create", "get", "result");
    }

    @Nullable
    private Object createViaStaticFactory(@NotNull Class<?> dialogClass, @NotNull Component title, @NotNull List<DialogComponent> components) {
        List<Object> payload = toPayload(components);

        Object dialog = invokeStaticWithArguments(dialogClass, "of", title, payload);
        if (dialog != null) {
            return dialog;
        }

        dialog = invokeStaticWithArguments(dialogClass, "create", title, payload);
        if (dialog != null) {
            return dialog;
        }

        dialog = invokeStaticWithArguments(dialogClass, "of", payload);
        if (dialog != null) {
            return dialog;
        }

        return invokeStaticWithArguments(dialogClass, "create", payload);
    }

    @Nullable
    private Object createViaConstructor(@NotNull Class<?> dialogClass, @NotNull Component title, @NotNull List<DialogComponent> components) {
        List<Object> payload = toPayload(components);

        Object dialog = instantiate(dialogClass, title, payload);
        if (dialog != null) {
            return dialog;
        }

        dialog = instantiate(dialogClass, payload);
        if (dialog != null) {
            return dialog;
        }

        return instantiate(dialogClass, title);
    }

    @NotNull
    private List<Object> toPayload(@NotNull List<DialogComponent> components) {
        List<Object> payload = new ArrayList<>(components.size());
        for (DialogComponent component : components) {
            Object nativeComponent = createNativeComponent(component);
            payload.add(nativeComponent != null ? nativeComponent : toPayload(component));
        }
        return payload;
    }

    @NotNull
    private Map<String, Object> toPayload(@NotNull DialogComponent component) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", component.type);
        payload.put("label", component.label);
        if (component.value != null) {
            payload.put("value", component.value);
        }
        return payload;
    }

    @Nullable
    private Object createNativeComponent(@NotNull DialogComponent component) {
        String className = switch (component.type) {
            case "input" -> INPUT_CLASS;
            case "toggle" -> "org.spongepowered.api.ui.dialog.input.DialogToggle";
            case "slider" -> "org.spongepowered.api.ui.dialog.input.DialogSlider";
            case "dropdown" -> "org.spongepowered.api.ui.dialog.input.DialogDropdown";
            case "button" -> "org.spongepowered.api.ui.dialog.action.DialogButton";
            case "text" -> "org.spongepowered.api.ui.dialog.content.DialogText";
            case "divider" -> "org.spongepowered.api.ui.dialog.content.DialogDivider";
            default -> null;
        };

        if (className == null) {
            return null;
        }

        final Class<?> componentClass;
        try {
            componentClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }

        Object nativeComponent = invokeStaticWithArguments(componentClass, "of", component.label, component.value);
        if (nativeComponent != null) {
            return nativeComponent;
        }

        nativeComponent = invokeStaticWithArguments(componentClass, "create", component.label, component.value);
        if (nativeComponent != null) {
            return nativeComponent;
        }

        nativeComponent = instantiate(componentClass, component.label, component.value);
        if (nativeComponent != null) {
            return nativeComponent;
        }

        return instantiate(componentClass, component.label);
    }

    @Nullable
    private Object invokeStaticNoArg(@NotNull Class<?> type, @NotNull String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = type.getMethod(methodName);
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                return method.invoke(null);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private boolean invokeInstanceWithSingleArgument(@NotNull Object target, @Nullable Object argument, @NotNull String... methodNames) {
        Class<?> targetType = target.getClass();
        for (String methodName : methodNames) {
            Method method = findCompatibleMethod(targetType, methodName, argument, false);
            if (method == null) {
                continue;
            }
            try {
                method.invoke(target, argument);
                return true;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }

    @Nullable
    private Object invokeInstanceNoArg(@NotNull Object target, @NotNull String... methodNames) {
        Class<?> targetType = target.getClass();
        for (String methodName : methodNames) {
            try {
                Method method = targetType.getMethod(methodName);
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    @Nullable
    private Object invokeStaticWithArguments(@NotNull Class<?> type, @NotNull String methodName, @Nullable Object... arguments) {
        Method method = findCompatibleMethod(type, methodName, arguments, true);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(null, arguments);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    private Object instantiate(@NotNull Class<?> type, @Nullable Object... arguments) {
        Constructor<?> constructor = findCompatibleConstructor(type, arguments);
        if (constructor == null) {
            return null;
        }

        try {
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    private Method findCompatibleMethod(@NotNull Class<?> type, @NotNull String methodName, @Nullable Object argument, boolean requireStatic) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers()) != requireStatic) {
                continue;
            }
            if (isCompatible(method.getParameterTypes()[0], argument)) {
                return method;
            }
        }
        return null;
    }

    @Nullable
    private Method findCompatibleMethod(@NotNull Class<?> type, @NotNull String methodName, @Nullable Object[] arguments, boolean requireStatic) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers()) != requireStatic) {
                continue;
            }
            if (isCompatible(method.getParameterTypes(), arguments)) {
                return method;
            }
        }
        return null;
    }

    @Nullable
    private Constructor<?> findCompatibleConstructor(@NotNull Class<?> type, @Nullable Object[] arguments) {
        for (Constructor<?> constructor : type.getConstructors()) {
            if (isCompatible(constructor.getParameterTypes(), arguments)) {
                return constructor;
            }
        }
        return null;
    }

    private boolean isCompatible(@NotNull Class<?>[] parameterTypes, @Nullable Object[] arguments) {
        if (arguments == null) {
            return parameterTypes.length == 0;
        }

        if (parameterTypes.length != arguments.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            if (!isCompatible(parameterTypes[i], arguments[i])) {
                return false;
            }
        }

        return true;
    }

    private boolean isCompatible(@NotNull Class<?> parameterType, @Nullable Object argument) {
        if (argument == null) {
            return !parameterType.isPrimitive();
        }

        Class<?> boxedType = box(parameterType);
        return boxedType.isAssignableFrom(argument.getClass());
    }

    @NotNull
    private Class<?> box(@NotNull Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }

    static final class DialogComponent {
        final String type;
        final Component label;
        final Object value;

        DialogComponent(String type, Component label, Object value) {
            this.type = type;
            this.label = label;
            this.value = value;
        }
    }
}
