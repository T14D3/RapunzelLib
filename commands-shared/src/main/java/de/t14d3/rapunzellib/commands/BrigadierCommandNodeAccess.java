package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reflective utility for removing commands from a Brigadier {@link RootCommandNode}.
 * <p>
 * Accesses the {@code children}, {@code literals}, and {@code arguments} maps
 * via reflection to support runtime command un-registration.
 */
final class BrigadierCommandNodeAccess {
    
    private static final Field CHILDREN = field("children");
    
    private static final Field LITERALS = field("literals");
    
    private static final Field ARGUMENTS = field("arguments");

    private BrigadierCommandNodeAccess() {
    }

    /**
     * Removes all commands matching the given labels from the root node.
     *
     * @param root   the root command node
     * @param labels the command labels to remove
     */
    static void removeCommands(@NotNull RootCommandNode<?> root, @NotNull Collection<String> labels) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(labels, "labels");
        if (labels.isEmpty()) {
            return;
        }

        for (String label : labels) {
            removeCommand(root, label);
        }
    }

    private static void removeCommand(@NotNull RootCommandNode<?> root, @NotNull String label) {
        if (label.isBlank()) {
            return;
        }

        remove(children(root), label);
        remove(literals(root), label);
        remove(arguments(root), label);
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, CommandNode<?>> children(@NotNull RootCommandNode<?> root) {
        return (Map<String, CommandNode<?>>) get(CHILDREN, root);
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, CommandNode<?>> literals(@NotNull RootCommandNode<?> root) {
        return (Map<String, CommandNode<?>>) get(LITERALS, root);
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, CommandNode<?>> arguments(@NotNull RootCommandNode<?> root) {
        return (Map<String, CommandNode<?>>) get(ARGUMENTS, root);
    }

    private static void remove(@NotNull Map<String, CommandNode<?>> map, @NotNull String label) {
        map.remove(label);
        for (String key : namespacedKeys(map.keySet(), label)) {
            map.remove(key);
        }
    }

    private static @NotNull Set<String> namespacedKeys(@NotNull Collection<String> keys, @NotNull String label) {
        Set<String> matches = new LinkedHashSet<>();
        for (String key : keys) {
            int separator = key.indexOf(':');
            if (separator < 0 || separator == key.length() - 1) {
                continue;
            }
            if (key.substring(separator + 1).equalsIgnoreCase(label)) {
                matches.add(key);
            }
        }
        return matches;
    }

    private static @NotNull Field field(@NotNull String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access Brigadier command node field: " + name, exception);
        }
    }

    private static Object get(@NotNull Field field, @NotNull Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to read Brigadier command node state", exception);
        }
    }
}
