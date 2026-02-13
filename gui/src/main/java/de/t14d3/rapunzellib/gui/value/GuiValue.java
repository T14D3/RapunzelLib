package de.t14d3.rapunzellib.gui.value;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface GuiValue permits GuiValue.StringValue, GuiValue.BooleanValue, GuiValue.NumberValue {
    @NotNull Kind kind();

    static @Nullable GuiValue from(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof GuiValue guiValue) {
            return guiValue;
        }
        if (value instanceof String string) {
            return of(string);
        }
        if (value instanceof Boolean bool) {
            return of(bool);
        }
        if (value instanceof Number number) {
            return of(number.doubleValue());
        }
        return null;
    }

    static @NotNull StringValue of(@NotNull String value) {
        return new StringValue(value);
    }

    static @NotNull BooleanValue of(boolean value) {
        return new BooleanValue(value);
    }

    static @NotNull NumberValue of(double value) {
        return new NumberValue(value);
    }

    default @Nullable String stringValue() {
        return switch (this) {
            case StringValue stringValue -> stringValue.value();
            case BooleanValue booleanValue -> Boolean.toString(booleanValue.value());
            case NumberValue numberValue -> Double.toString(numberValue.value());
        };
    }

    default @NotNull String stringValue(@NotNull String fallback) {
        String value = stringValue();
        return value != null ? value : fallback;
    }

    default @Nullable Boolean booleanValue() {
        return switch (this) {
            case BooleanValue booleanValue -> booleanValue.value();
            case StringValue stringValue -> Boolean.parseBoolean(stringValue.value());
            case NumberValue numberValue -> numberValue.value() != 0.0d;
        };
    }

    default boolean booleanValue(boolean fallback) {
        Boolean value = booleanValue();
        return value != null ? value : fallback;
    }

    default @Nullable Double numberValue() {
        return switch (this) {
            case NumberValue numberValue -> numberValue.value();
            case BooleanValue booleanValue -> booleanValue.value() ? 1.0d : 0.0d;
            case StringValue stringValue -> parseNumber(stringValue.value());
        };
    }

    default float floatValue(float fallback) {
        Double value = numberValue();
        return value != null ? value.floatValue() : fallback;
    }

    default double doubleValue(double fallback) {
        Double value = numberValue();
        return value != null ? value : fallback;
    }

    private static @Nullable Double parseNumber(@NotNull String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    enum Kind {
        STRING,
        BOOLEAN,
        NUMBER
    }

    record StringValue(@NotNull String value) implements GuiValue {
        @Override
        public @NotNull Kind kind() {
            return Kind.STRING;
        }
    }

    record BooleanValue(boolean value) implements GuiValue {
        @Override
        public @NotNull Kind kind() {
            return Kind.BOOLEAN;
        }
    }

    record NumberValue(double value) implements GuiValue {
        @Override
        public @NotNull Kind kind() {
            return Kind.NUMBER;
        }
    }
}
