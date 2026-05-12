package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.nbt.RNbtCodec;
import de.t14d3.rapunzellib.nbt.RNbtCodecs;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.RNbtPath;
import de.t14d3.rapunzellib.nbt.RNbtSchema;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.nbt.generated.RItemNbt;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Central repository of well-known {@link RNbtField} and {@link RNbtPath} constants
 * for Minecraft item component data.
 * <p>
 * Delegates to the generated {@link de.t14d3.rapunzellib.nbt.generated.RItemNbt} schema
 * for concrete field definitions and provides convenience helpers for accessing
 * component and custom data sub-paths.</p>
 */
public final class RItemFields {
    /**
     * The full components compound field.
     */
    public static final RNbtField<RNbtCompound> COMPONENTS = RItemNbt.Fields.COMPONENTS;
    /**
     * The custom data compound field.
     */
    public static final RNbtField<RNbtCompound> CUSTOM_DATA = RItemNbt.Fields.CUSTOM_DATA;

    /**
     * Path to the custom name component.
     */
    public static final RNbtPath<Component> NAME = RItemNbt.Paths.COMPONENTS_CUSTOM_NAME;
    /**
     * Path to the lore component.
     */
    public static final RNbtPath<List<Component>> LORE = RItemNbt.Paths.COMPONENTS_LORE;
    /**
     * Path to the damage component.
     */
    public static final RNbtPath<Integer> DAMAGE = RItemNbt.Paths.COMPONENTS_DAMAGE;
    /**
     * Alias for DAMAGE.
     */
    public static final RNbtPath<Integer> DURABILITY = DAMAGE;
    /**
     * Path to the unbreakable component.
     */
    public static final RNbtPath<Boolean> UNBREAKABLE = RItemNbt.Paths.COMPONENTS_UNBREAKABLE;
    /**
     * Path to the custom model data component.
     */
    public static final RNbtPath<Integer> CUSTOM_MODEL_DATA = RItemNbt.Paths.COMPONENTS_CUSTOM_MODEL_DATA;
    /**
     * Path to the repair cost component.
     */
    public static final RNbtPath<Integer> REPAIR_COST = RItemNbt.Paths.COMPONENTS_REPAIR_COST;
    /**
     * Path to the enchantment glint override component.
     */
    public static final RNbtPath<Boolean> ENCHANTMENT_GLINT_OVERRIDE = RItemNbt.Paths.COMPONENTS_ENCHANTMENT_GLINT_OVERRIDE;

    /**
     * The full generated item NBT schema.
     */
    public static final RNbtSchema SCHEMA = RItemNbt.SCHEMA;

    private RItemFields() {
    }

    /**
     * Creates a path to a named component within the components compound.
     *
     * @param key the component key
     * @return a path to the component value
     */
    public static @NotNull RNbtPath<RNbtValue> component(@NotNull String key) {
        return path(RNbtCodecs.VALUE, COMPONENTS.key(), key);
    }

    /**
     * Creates a path to a named custom data entry.
     *
     * @param key the custom data key
     * @return a path to the value
     */
    public static @NotNull RNbtPath<RNbtValue> custom(@NotNull String key) {
        return path(RNbtCodecs.VALUE, CUSTOM_DATA.key(), key);
    }

    private static <T> @NotNull RNbtPath<T> path(@NotNull RNbtCodec<T> codec, @NotNull String first, String @NotNull ... rest) {
        RNbtPath<T> path = RNbtPath.of(codec, first);
        for (String key : rest) {
            path = path.key(key);
        }
        return path;
    }
}
