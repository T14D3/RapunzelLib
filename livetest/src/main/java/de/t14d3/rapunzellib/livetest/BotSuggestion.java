package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A single tab-completion suggestion returned by the server.
 *
 * @param match   the suggested completion string
 * @param tooltip a plain-text tooltip if the server provided one, or {@code null} if none
 */
public record BotSuggestion(@NotNull String match, @Nullable String tooltip) {

    public BotSuggestion {
        Objects.requireNonNull(match, "match");
    }

    public BotSuggestion(@NotNull String match) {
        this(match, null);
    }

    /**
     * A non-null, possibly-empty tooltip - convenience for tests that want a
     * plain string rather than a nullable one.
     */
    public @NotNull String tooltipOrEmpty() {
        return tooltip != null ? tooltip : "";
    }
}
