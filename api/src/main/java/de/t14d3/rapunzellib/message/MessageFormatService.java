package de.t14d3.rapunzellib.message;

import net.kyori.adventure.text.Component;

import java.util.Set;

public interface MessageFormatService {
    void reload();

    boolean contains(String key);

    Set<String> keys();

    String raw(String key);

    Component component(String key);

    Component component(String key, Placeholders placeholders);

    default Component component(MessageKey key) {
        return component(key.value());
    }

    default Component component(MessageKey key, Placeholders placeholders) {
        return component(key.value(), placeholders);
    }
}

