package de.t14d3.rapunzellib.gui.sponge.dialog;

import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValues;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogSubmissionProcessor;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

final class DialogHandler {

    private DialogHandler() {
    }

    static void openDialog(@NotNull ServerPlayer player, @NotNull Object dialog) {
        if (invokePlayerDialogMethod(player, dialog, "showDialog", "openDialog", "displayDialog", "show")) {
            return;
        }

        for (Method method : player.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
                continue;
            }

            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("dialog") || (!name.contains("open") && !name.contains("show") && !name.contains("display"))) {
                continue;
            }

            if (!method.getParameterTypes()[0].isAssignableFrom(dialog.getClass())) {
                continue;
            }

            try {
                method.invoke(player, dialog);
            } catch (ReflectiveOperationException ignored) {
            }
            return;
        }
    }

    static void closeDialog(@NotNull ServerPlayer player) {
        if (invokePlayerNoArgMethod(player, "closeDialog", "dismissDialog", "hideDialog")) {
            return;
        }

        for (Method method : player.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                continue;
            }

            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("dialog") || (!name.contains("close") && !name.contains("dismiss") && !name.contains("hide"))) {
                continue;
            }

            try {
                method.invoke(player);
            } catch (ReflectiveOperationException ignored) {
            }
            return;
        }
    }

    private static boolean invokePlayerDialogMethod(@NotNull ServerPlayer player, @NotNull Object dialog, @NotNull String... methodNames) {
        Class<?> dialogType = dialog.getClass();
        Class<?> playerType = player.getClass();

        for (String methodName : methodNames) {
            for (Method method : playerType.getMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isAssignableFrom(dialogType)) {
                    continue;
                }

                try {
                    method.invoke(player, dialog);
                    return true;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }

        return false;
    }

    private static boolean invokePlayerNoArgMethod(@NotNull ServerPlayer player, @NotNull String... methodNames) {
        Class<?> playerType = player.getClass();

        for (String methodName : methodNames) {
            try {
                Method method = playerType.getMethod(methodName);
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                    continue;
                }
                method.invoke(player);
                return true;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return false;
    }

    static void handleSubmission(@NotNull DialogRenderer.ActiveDialog active, @NotNull Map<String, GuiValue> values) {
        GuiDialogFieldValues submittedValues = de.t14d3.rapunzellib.gui.dialog.GuiDialogStateSupport.collectSubmittedValues(
            active.model,
            field -> values.get(field.key())
        );
        GuiDialogSubmissionProcessor.submit(active.model, submittedValues, active.context.player(), active.context.state());
    }
}
