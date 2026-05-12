package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.nbt.RNbtByteArray;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtIntArray;
import de.t14d3.rapunzellib.nbt.RNbtList;
import de.t14d3.rapunzellib.nbt.RNbtLongArray;
import de.t14d3.rapunzellib.nbt.RNbtPrimitive;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.nbt.SerializationException;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * I/O support for converting between Minecraft NBT tags and
 * Rapunzel's platform-independent {@link RNbtValue} tree representation.
 * <p>
 * Supports compressed binary serialization and recursive tree conversion
 * for all standard NBT types.
 */
public final class SharedNbtIoSupport {
    private SharedNbtIoSupport() {
    }

    /**
     * Serializes an {@link RNbtCompound} to a compressed byte array.
     *
     * @param nbt the compound to serialize
     * @return the compressed bytes
     * @throws SerializationException if serialization fails
     */
    public static byte[] serializeCompressed(@NotNull RNbtCompound nbt) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            NbtIo.writeCompressed(fromTree(nbt), dos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize NBT to bytes", e);
        }
    }

    /**
     * Deserializes a compressed byte array to an {@link RNbtCompound}.
     *
     * @param bytes the compressed bytes
     * @return the deserialized compound
     * @throws SerializationException if deserialization fails
     */
    public static @NotNull RNbtCompound deserializeCompressed(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {
            return toTree(NbtIo.readCompressed(dis, NbtAccounter.unlimitedHeap()));
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize NBT from bytes", e);
        }
    }

    /**
     * Converts a Minecraft {@link CompoundTag} to an {@link RNbtCompound}.
     *
     * @param nbt the native compound
     * @return the shared compound
     */
    public static @NotNull RNbtCompound toTree(@NotNull CompoundTag nbt) {
        return toTreeValue(nbt).asCompound();
    }

    /**
     * Converts an {@link RNbtCompound} to a Minecraft {@link CompoundTag}.
     *
     * @param nbt the shared compound
     * @return the native compound
     */
    public static @NotNull CompoundTag fromTree(@NotNull RNbtCompound nbt) {
        return (CompoundTag) toNativeTag(nbt);
    }

    /**
     * Converts any Minecraft {@link Tag} to the shared tree representation.
     *
     * @param tag the native tag
     * @return the shared value
     */
    public static @NotNull RNbtValue toTreeValue(@NotNull Tag tag) {
        return switch (tag.getId()) {
            case Tag.TAG_BYTE -> RNbtPrimitive.ofByte(((ByteTag) tag).value());
            case Tag.TAG_SHORT -> RNbtPrimitive.ofShort(((ShortTag) tag).value());
            case Tag.TAG_INT -> RNbtPrimitive.ofInt(((IntTag) tag).value());
            case Tag.TAG_LONG -> RNbtPrimitive.ofLong(((LongTag) tag).value());
            case Tag.TAG_FLOAT -> RNbtPrimitive.ofFloat(((FloatTag) tag).value());
            case Tag.TAG_DOUBLE -> RNbtPrimitive.ofDouble(((DoubleTag) tag).value());
            case Tag.TAG_BYTE_ARRAY -> new RNbtByteArray(((ByteArrayTag) tag).getAsByteArray());
            case Tag.TAG_STRING -> RNbtPrimitive.ofString(((StringTag) tag).value());
            case Tag.TAG_LIST -> toTreeList((ListTag) tag);
            case Tag.TAG_COMPOUND -> toTreeCompound((CompoundTag) tag);
            case Tag.TAG_INT_ARRAY -> new RNbtIntArray(((IntArrayTag) tag).getAsIntArray());
            case Tag.TAG_LONG_ARRAY -> new RNbtLongArray(((LongArrayTag) tag).getAsLongArray());
            default -> throw new SerializationException("Unsupported Shared NBT tag id: " + tag.getId());
        };
    }

    /**
     * Converts any shared {@link RNbtValue} to a Minecraft {@link Tag}.
     *
     * @param value the shared value
     * @return the native tag
     */
    public static @NotNull Tag toNativeTag(@NotNull RNbtValue value) {
        return switch (value) {
            case RNbtPrimitive primitive -> toNativePrimitive(primitive);
            case RNbtCompound compound -> toNativeCompound(compound);
            case RNbtList list -> toNativeList(list);
            case RNbtByteArray byteArray -> new ByteArrayTag(byteArray.value());
            case RNbtIntArray intArray -> new IntArrayTag(intArray.value());
            case RNbtLongArray longArray -> new LongArrayTag(longArray.value());
        };
    }

    /**
     * Converts a native CompoundTag to an RNbtCompound.
     *
     * @param tag the native compound
     * @return the shared compound
     */
    private static @NotNull RNbtCompound toTreeCompound(@NotNull CompoundTag tag) {
        LinkedHashMap<String, RNbtValue> entries = new LinkedHashMap<>();
        for (String key : tag.keySet()) {
            Tag child = tag.get(key);
            if (child != null) {
                entries.put(key, toTreeValue(child));
            }
        }
        return RNbtCompound.of(entries);
    }

    /**
     * Converts a native ListTag to an RNbtList.
     *
     * @param tag the native list
     * @return the shared list
     */
    private static @NotNull RNbtList toTreeList(@NotNull ListTag tag) {
        if (tag.isEmpty()) {
            return RNbtList.empty();
        }
        RNbtValue first = toTreeValue(tag.getFirst());
        RNbtList list = RNbtList.of(first.type(), java.util.List.of(first));
        for (int i = 1; i < tag.size(); i++) {
            list = list.add(toTreeValue(tag.get(i)));
        }
        return list;
    }

    /**
     * Converts a shared primitive to a native NBT primitive tag.
     *
     * @param primitive the shared primitive
     * @return the native tag
     */
    private static @NotNull Tag toNativePrimitive(@NotNull RNbtPrimitive primitive) {
        return switch (primitive.type()) {
            case BYTE -> ByteTag.valueOf(primitive.byteValue());
            case SHORT -> ShortTag.valueOf(primitive.shortValue());
            case INT -> IntTag.valueOf(primitive.intValue());
            case LONG -> LongTag.valueOf(primitive.longValue());
            case FLOAT -> FloatTag.valueOf(primitive.floatValue());
            case DOUBLE -> DoubleTag.valueOf(primitive.doubleValue());
            case STRING -> StringTag.valueOf(primitive.stringValue());
            default -> throw new SerializationException("Unsupported primitive RNbt type: " + primitive.type());
        };
    }

    /**
     * Converts a shared compound to a native CompoundTag.
     *
     * @param compound the shared compound
     * @return the native compound
     */
    private static @NotNull CompoundTag toNativeCompound(@NotNull RNbtCompound compound) {
        CompoundTag tag = new CompoundTag();
        compound.asMap().forEach((key, value) -> tag.put(key, toNativeTag(value)));
        return tag;
    }

    /**
     * Converts a shared list to a native ListTag.
     *
     * @param list the shared list
     * @return the native list
     */
    private static @NotNull ListTag toNativeList(@NotNull RNbtList list) {
        ListTag tag = new ListTag();
        for (RNbtValue element : list.values()) {
            tag.add(toNativeTag(element));
        }
        return tag;
    }
}
