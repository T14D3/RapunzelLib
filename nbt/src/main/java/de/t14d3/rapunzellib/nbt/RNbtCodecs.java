package de.t14d3.rapunzellib.nbt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A utility class providing commonly used {@link RNbtCodec} instances for standard Java types
 * and Minecraft-specific types like Adventure {@link Component}.
 * <p>
 * All codecs are stateless and safe to reuse.</p>
 */
public final class RNbtCodecs {
    /**
     * A pass-through codec that encodes/decodes any {@link RNbtValue} as-is.
     */
    public static final RNbtCodec<RNbtValue> VALUE = new RNbtCodec<>() {
        @Override
        public @NotNull RNbtValue decode(@NotNull RNbtValue value) {
            return Objects.requireNonNull(value, "value");
        }

        @Override
        public @NotNull RNbtValue encode(@NotNull RNbtValue value) {
            return Objects.requireNonNull(value, "value");
        }
    };

    /**
     * A codec for {@link RNbtCompound} values, requiring the exact type.
     */
    public static final RNbtCodec<RNbtCompound> COMPOUND = RNbtCodec.of(
        RNbtType.COMPOUND,
        value -> Objects.requireNonNull(value, "value"),
        value -> value.asCompound()
    );

    /**
     * A codec for {@link RNbtList} values, requiring the exact type.
     */
    public static final RNbtCodec<RNbtList> LIST = RNbtCodec.of(
        RNbtType.LIST,
        value -> Objects.requireNonNull(value, "value"),
        value -> value.asList()
    );

    /**
     * A codec for {@link String} values stored as NBT strings.
     */
    public static final RNbtCodec<String> STRING = RNbtCodec.of(
        RNbtType.STRING,
        RNbtPrimitive::ofString,
        value -> value.asPrimitive().stringValue()
    );

    /**
     * A codec for {@link Byte} values stored as NBT bytes.
     */
    public static final RNbtCodec<Byte> BYTE = RNbtCodec.of(
        RNbtType.BYTE,
        RNbtPrimitive::ofByte,
        value -> Byte.valueOf(value.asPrimitive().byteValue())
    );

    /**
     * A codec for {@link Short} values stored as NBT shorts.
     */
    public static final RNbtCodec<Short> SHORT = RNbtCodec.of(
        RNbtType.SHORT,
        RNbtPrimitive::ofShort,
        value -> Short.valueOf(value.asPrimitive().shortValue())
    );

    /**
     * A codec for {@link Integer} values stored as NBT ints.
     */
    public static final RNbtCodec<Integer> INT = RNbtCodec.of(
        RNbtType.INT,
        RNbtPrimitive::ofInt,
        value -> Integer.valueOf(value.asPrimitive().intValue())
    );

    /**
     * A codec for {@link Long} values stored as NBT longs.
     */
    public static final RNbtCodec<Long> LONG = RNbtCodec.of(
        RNbtType.LONG,
        RNbtPrimitive::ofLong,
        value -> Long.valueOf(value.asPrimitive().longValue())
    );

    /**
     * A codec for {@link Float} values stored as NBT floats.
     */
    public static final RNbtCodec<Float> FLOAT = RNbtCodec.of(
        RNbtType.FLOAT,
        RNbtPrimitive::ofFloat,
        value -> Float.valueOf(value.asPrimitive().floatValue())
    );

    /**
     * A codec for {@link Double} values stored as NBT doubles.
     */
    public static final RNbtCodec<Double> DOUBLE = RNbtCodec.of(
        RNbtType.DOUBLE,
        RNbtPrimitive::ofDouble,
        value -> Double.valueOf(value.asPrimitive().doubleValue())
    );

    /**
     * A codec for {@link Boolean} values stored as NBT bytes (0/1).
     */
    public static final RNbtCodec<Boolean> BOOLEAN = RNbtCodec.of(
        RNbtType.BYTE,
        RNbtPrimitive::ofBoolean,
        value -> Boolean.valueOf(value.asPrimitive().booleanValue())
    );

    /**
     * A codec for {@code byte[]} values stored as NBT byte arrays.
     */
    public static final RNbtCodec<byte[]> BYTE_ARRAY = RNbtCodec.of(
        RNbtType.BYTE_ARRAY,
        RNbtByteArray::new,
        value -> ((RNbtByteArray) requireType(value, RNbtType.BYTE_ARRAY)).value()
    );

    /**
     * A codec for {@code int[]} values stored as NBT int arrays.
     */
    public static final RNbtCodec<int[]> INT_ARRAY = RNbtCodec.of(
        RNbtType.INT_ARRAY,
        RNbtIntArray::new,
        value -> ((RNbtIntArray) requireType(value, RNbtType.INT_ARRAY)).value()
    );

    /**
     * A codec for {@code long[]} values stored as NBT long arrays.
     */
    public static final RNbtCodec<long[]> LONG_ARRAY = RNbtCodec.of(
        RNbtType.LONG_ARRAY,
        RNbtLongArray::new,
        value -> ((RNbtLongArray) requireType(value, RNbtType.LONG_ARRAY)).value()
    );

    /**
     * A codec for Adventure {@link Component} values serialized via Gson JSON strings.
     */
    public static final RNbtCodec<Component> COMPONENT = RNbtCodec.of(
        RNbtType.STRING,
        value -> RNbtPrimitive.ofString(GsonComponentSerializer.gson().serialize(value)),
        value -> GsonComponentSerializer.gson().deserialize(STRING.decode(value))
    );

    /**
     * A codec for lists of strings.
     */
    public static final RNbtCodec<List<String>> STRING_LIST = listOf(STRING);
    /**
     * A codec for lists of Adventure {@link Component Components}.
     */
    public static final RNbtCodec<List<Component>> COMPONENT_LIST = listOf(COMPONENT);

    private RNbtCodecs() {
    }

    /**
     * Creates a codec for {@code List<T>} using the given element codec.
     * The list is stored as an NBT list of the element codec's encoded type.
     *
     * @param <T>           the element type
     * @param elementCodec  the codec for individual elements
     * @return a codec for lists
     */
    public static <T> @NotNull RNbtCodec<List<T>> listOf(@NotNull RNbtCodec<T> elementCodec) {
        Objects.requireNonNull(elementCodec, "elementCodec");
        return RNbtCodec.of(
            RNbtType.LIST,
            values -> {
                RNbtListBuilder builder = RNbtList.builder(elementCodec.encodedType());
                values.forEach(value -> builder.add(elementCodec.encode(value)));
                return builder.build();
            },
            value -> {
                RNbtList list = value.asList();
                ArrayList<T> decoded = new ArrayList<>(list.size());
                for (RNbtValue element : list.values()) {
                    decoded.add(elementCodec.decode(element));
                }
                return List.copyOf(decoded);
            }
        );
    }

    private static @NotNull RNbtValue requireType(@NotNull RNbtValue value, @NotNull RNbtType type) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(type, "type");
        if (value.type() != type) {
            throw new IllegalArgumentException("Expected NBT type " + type + " but got " + value.type());
        }
        return value;
    }
}
