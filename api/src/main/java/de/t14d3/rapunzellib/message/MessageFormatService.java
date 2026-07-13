package de.t14d3.rapunzellib.message;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Service for formatting and resolving messages with placeholders.
 */
public interface MessageFormatService {
    /**
     * Reloads message sources from disk.
     */
    void reload();

    /** Checks whether the given key exists in the message registry. */
    boolean contains(@NotNull String key);

    /** Returns all registered message keys. */
    @NotNull Set<String> keys();

    /** Returns the raw (unformatted) message string for the given key. */
    @NotNull String raw(@NotNull String key);

    /**
     * Resolves the message for the given key as a formatted component.
     *
     * @param key the message key
     * @return the formatted component, or a fallback if the key is not found
     */
    @NotNull Component component(@NotNull String key);

    /**
     * Resolves the message for the given key with placeholders applied.
     *
     * @param key          the message key
     * @param placeholders the placeholders to substitute into the message template
     * @return the formatted component with placeholders resolved
     */
    @NotNull Component component(@NotNull String key, @NotNull Placeholders placeholders);

    /**
     * Resolves the message for the given key as a formatted component.
     *
     * @param key the message key
     * @return formatted component
     */
    default @NotNull Component component(@NotNull MessageKey key) {
        return component(key.value());
    }

    /**
     * Resolves the message for the given key with placeholders applied.
     *
     * @param key the message key
     * @param placeholders the placeholders to apply
     * @return formatted component
     */
    default @NotNull Component component(@NotNull MessageKey key, @NotNull Placeholders placeholders) {
        return component(key.value(), placeholders);
    }
}

