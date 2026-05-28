package de.t14d3.rapunzellib;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class Utils {
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final JSONComponentSerializer JSON_COMPONENT_SERIALIZER = JSONComponentSerializer.json();
    private Utils() {};

    public static String toString(Component component) {
        return PLAIN_TEXT_SERIALIZER.serialize(component);
    }

    public static String toJson(Component component) {
        return JSON_COMPONENT_SERIALIZER.serialize(component);
    }
}
