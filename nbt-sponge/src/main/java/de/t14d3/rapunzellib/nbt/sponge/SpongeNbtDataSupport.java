package de.t14d3.rapunzellib.nbt.sponge;

import de.t14d3.rapunzellib.nbt.RNbtByteArray;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtIntArray;
import de.t14d3.rapunzellib.nbt.RNbtList;
import de.t14d3.rapunzellib.nbt.RNbtLongArray;
import de.t14d3.rapunzellib.nbt.RNbtPrimitive;
import de.t14d3.rapunzellib.nbt.RNbtType;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.nbt.SerializationException;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class SpongeNbtDataSupport {
    private SpongeNbtDataSupport() {
    }

    static @NotNull RNbtCompound toTree(@NotNull DataView view) {
        LinkedHashMap<String, RNbtValue> entries = new LinkedHashMap<>();
        for (Map.Entry<DataQuery, Object> entry : Objects.requireNonNull(view, "view").values(false).entrySet()) {
            entries.put(key(entry.getKey()), toTreeValue(entry.getValue()));
        }
        return RNbtCompound.of(entries);
    }

    static @NotNull DataContainer fromTree(@NotNull RNbtCompound compound) {
        DataContainer container = DataContainer.createNew();
        compound.asMap().forEach((key, value) -> container.set(DataQuery.of(key), fromTreeValue(value)));
        return container;
    }

    private static @NotNull RNbtValue toTreeValue(@NotNull Object value) {
        Objects.requireNonNull(value, "value");
        if (value instanceof DataView dataView) {
            return toTree(dataView);
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, RNbtValue> entries = new LinkedHashMap<>();
            map.forEach((key, child) -> entries.put(stringKey(key), toTreeValue(Objects.requireNonNull(child, () -> "value for key '" + key + "'"))));
            return RNbtCompound.of(entries);
        }
        if (value instanceof List<?> list) {
            return toTreeList(list);
        }
        if (value instanceof String string) {
            return RNbtPrimitive.ofString(string);
        }
        if (value instanceof Byte number) {
            return RNbtPrimitive.ofByte(number.byteValue());
        }
        if (value instanceof Short number) {
            return RNbtPrimitive.ofShort(number.shortValue());
        }
        if (value instanceof Integer number) {
            return RNbtPrimitive.ofInt(number.intValue());
        }
        if (value instanceof Long number) {
            return RNbtPrimitive.ofLong(number.longValue());
        }
        if (value instanceof Float number) {
            return RNbtPrimitive.ofFloat(number.floatValue());
        }
        if (value instanceof Double number) {
            return RNbtPrimitive.ofDouble(number.doubleValue());
        }
        if (value instanceof Boolean bool) {
            return RNbtPrimitive.ofBoolean(bool.booleanValue());
        }
        if (value instanceof byte[] bytes) {
            return new RNbtByteArray(bytes);
        }
        if (value instanceof int[] ints) {
            return new RNbtIntArray(ints);
        }
        if (value instanceof long[] longs) {
            return new RNbtLongArray(longs);
        }
        throw new SerializationException("Unsupported Sponge data value: " + value.getClass().getName());
    }

    private static @NotNull RNbtList toTreeList(@NotNull List<?> values) {
        if (values.isEmpty()) {
            return RNbtList.empty();
        }
        ArrayList<RNbtValue> converted = new ArrayList<>(values.size());
        for (Object value : values) {
            converted.add(toTreeValue(Objects.requireNonNull(value, "list element")));
        }
        RNbtType elementType = converted.getFirst().type();
        return RNbtList.of(elementType, converted);
    }

    private static @NotNull Object fromTreeValue(@NotNull RNbtValue value) {
        return switch (value) {
            case RNbtPrimitive primitive -> switch (primitive.type()) {
                case STRING -> primitive.stringValue();
                case BYTE -> Byte.valueOf(primitive.byteValue());
                case SHORT -> Short.valueOf(primitive.shortValue());
                case INT -> Integer.valueOf(primitive.intValue());
                case LONG -> Long.valueOf(primitive.longValue());
                case FLOAT -> Float.valueOf(primitive.floatValue());
                case DOUBLE -> Double.valueOf(primitive.doubleValue());
                default -> throw new SerializationException("Unsupported primitive RNbt type: " + primitive.type());
            };
            case RNbtCompound compound -> fromTree(compound);
            case RNbtList list -> fromTreeList(list);
            case RNbtByteArray byteArray -> byteArray.value();
            case RNbtIntArray intArray -> intArray.value();
            case RNbtLongArray longArray -> longArray.value();
        };
    }

    private static @NotNull List<Object> fromTreeList(@NotNull RNbtList list) {
        ArrayList<Object> values = new ArrayList<>(list.size());
        for (RNbtValue element : list.values()) {
            values.add(fromTreeValue(element));
        }
        return List.copyOf(values);
    }

    private static @NotNull String key(@NotNull DataQuery query) {
        List<String> parts = Objects.requireNonNull(query, "query").parts();
        return parts.isEmpty() ? "" : parts.getLast();
    }

    private static @NotNull String stringKey(@NotNull Object key) {
        if (key instanceof DataQuery dataQuery) {
            return key(dataQuery);
        }
        return String.valueOf(key);
    }
}
