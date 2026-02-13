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

public final class RItemFields {
    public static final RNbtField<RNbtCompound> COMPONENTS = RItemNbt.Fields.COMPONENTS;
    public static final RNbtField<RNbtCompound> CUSTOM_DATA = RItemNbt.Fields.CUSTOM_DATA;

    public static final RNbtPath<Component> NAME = RItemNbt.Paths.COMPONENTS_CUSTOM_NAME;
    public static final RNbtPath<List<Component>> LORE = RItemNbt.Paths.COMPONENTS_LORE;
    public static final RNbtPath<Integer> DAMAGE = RItemNbt.Paths.COMPONENTS_DAMAGE;
    public static final RNbtPath<Integer> DURABILITY = DAMAGE;
    public static final RNbtPath<Boolean> UNBREAKABLE = RItemNbt.Paths.COMPONENTS_UNBREAKABLE;
    public static final RNbtPath<Integer> CUSTOM_MODEL_DATA = RItemNbt.Paths.COMPONENTS_CUSTOM_MODEL_DATA;
    public static final RNbtPath<Integer> REPAIR_COST = RItemNbt.Paths.COMPONENTS_REPAIR_COST;
    public static final RNbtPath<Boolean> ENCHANTMENT_GLINT_OVERRIDE = RItemNbt.Paths.COMPONENTS_ENCHANTMENT_GLINT_OVERRIDE;

    public static final RNbtSchema SCHEMA = RItemNbt.SCHEMA;

    private RItemFields() {
    }

    public static @NotNull RNbtPath<RNbtValue> component(@NotNull String key) {
        return path(RNbtCodecs.VALUE, COMPONENTS.key(), key);
    }

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
