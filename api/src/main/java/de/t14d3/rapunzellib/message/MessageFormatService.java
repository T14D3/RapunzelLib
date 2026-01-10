package de.t14d3.rapunzellib.message;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface MessageFormatService {
    void reload();

    boolean contains(@NotNull String key);

    @NotNull Set<String> keys();

    @NotNull String raw(@NotNull String key);

    @NotNull Component component(@NotNull String key);

    @NotNull Component component(@NotNull String key, @NotNull Placeholders placeholders);

    default @NotNull Component component(@NotNull MessageKey key) {
        return component(key.value());
    }

    default @NotNull Component component(@NotNull MessageKey key, @NotNull Placeholders placeholders) {
        return component(key.value(), placeholders);
    }
}

