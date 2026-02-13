package de.t14d3.rapunzellib.gradle;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RapunzelLibPluginDefaults {
    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Za-z0-9]+");

    private RapunzelLibPluginDefaults() {
    }

    public static String defaultRegistryCatalogClassName(String name) {
        StringBuilder builder = new StringBuilder();
        for (String segment : NON_ALNUM.split(name)) {
            if (segment.isBlank()) {
                continue;
            }
            String normalized = segment.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(normalized.charAt(0)));
            builder.append(normalized.substring(1));
        }
        return builder.isEmpty() ? "GeneratedRegistryCatalog" : builder.toString();
    }
}
