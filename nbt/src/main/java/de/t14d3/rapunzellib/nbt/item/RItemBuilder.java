package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.attachments.RAttachmentKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.RNbtPath;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A mutable builder for constructing {@link RItem} instances fluently.
 * <p>
 * Provides convenience methods for setting item properties, NBT fields, attachments, and more.</p>
 */
public class RItemBuilder {
    RRegistryRef<RItemType> typeRef;
    int amount = 1;
    RNbtCompound data = RNbtCompound.empty();

    /**
     * Sets the item type reference.
     *
     * @param typeRef the type reference
     * @return this builder
     */
    public RItemBuilder typeRef(@NotNull RRegistryRef<RItemType> typeRef) {
        this.typeRef = Objects.requireNonNull(typeRef, "typeRef");
        return this;
    }

    /**
     * Sets the item type by key.
     *
     * @param typeKey the type key
     * @return this builder
     */
    public RItemBuilder typeKey(@NotNull RKey typeKey) {
        return typeRef(RItemType.ref(typeKey));
    }

    /**
     * Sets the item type by string key.
     *
     * @param typeKey the type key string
     * @return this builder
     */
    public RItemBuilder typeKey(@NotNull String typeKey) {
        return typeKey(RKey.of(typeKey));
    }

    /**
     * Sets the item material by string (alias for typeKey).
     *
     * @param material the material string
     * @return this builder
     */
    public RItemBuilder material(@NotNull String material) {
        return typeKey(material);
    }

    /**
     * Sets the item material by key (alias for typeKey).
     *
     * @param material the material key
     * @return this builder
     */
    public RItemBuilder material(@NotNull RKey material) {
        return typeKey(material);
    }

    /**
     * Sets the stack amount.
     *
     * @param amount the amount
     * @return this builder
     */
    public RItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Sets the NBT data compound.
     *
     * @param data the data compound
     * @return this builder
     */
    public RItemBuilder data(@NotNull RNbtCompound data) {
        this.data = Objects.requireNonNull(data, "data");
        return this;
    }

    /**
     * Writes a typed field into the data compound.
     *
     * @param <T>   the value type
     * @param field the field descriptor
     * @param value the value
     * @return this builder
     */
    public <T> RItemBuilder set(@NotNull RNbtField<T> field, @NotNull T value) {
        data = Objects.requireNonNull(field, "field").write(data, value);
        return this;
    }

    /**
     * Writes a typed path into the data compound.
     *
     * @param <T>   the value type
     * @param path the path descriptor
     * @param value the value
     * @return this builder
     */
    public <T> RItemBuilder set(@NotNull RNbtPath<T> path, @NotNull T value) {
        data = Objects.requireNonNull(path, "path").write(data, value);
        return this;
    }

    /**
     * Removes a field from the data compound.
     *
     * @param field the field to remove
     * @return this builder
     */
    public RItemBuilder remove(@NotNull RNbtField<?> field) {
        data = Objects.requireNonNull(field, "field").remove(data);
        return this;
    }

    /**
     * Removes a path from the data compound.
     *
     * @param path the path to remove
     * @return this builder
     */
    public RItemBuilder remove(@NotNull RNbtPath<?> path) {
        data = Objects.requireNonNull(path, "path").remove(data);
        return this;
    }

    /**
     * Sets the custom name (or removes it if null).
     *
     * @param name the name component, or null to clear
     * @return this builder
     */
    public RItemBuilder name(@Nullable Component name) {
        return name == null ? remove(RItemFields.NAME) : set(RItemFields.NAME, name);
    }

    /**
     * Sets the lore lines (or removes if empty).
     *
     * @param lore the lore lines
     * @return this builder
     */
    public RItemBuilder lore(@NotNull List<Component> lore) {
        List<Component> lines = new ArrayList<>(Objects.requireNonNull(lore, "lore"));
        return lines.isEmpty() ? remove(RItemFields.LORE) : set(RItemFields.LORE, List.copyOf(lines));
    }

    /**
     * Sets the lore lines from varargs (or removes if empty).
     *
     * @param lines the lore components
     * @return this builder
     */
    public RItemBuilder lore(@NotNull Component... lines) {
        return lore(Arrays.asList(lines));
    }

    /**
     * Appends a single lore line.
     *
     * @param line the component to append
     * @return this builder
     */
    public RItemBuilder addLore(@NotNull Component line) {
        ArrayList<Component> lines = new ArrayList<>(RItemFields.LORE.read(data).orElse(List.of()));
        lines.add(Objects.requireNonNull(line, "line"));
        data = RItemFields.LORE.write(data, List.copyOf(lines));
        return this;
    }

    /**
     * Sets the full components compound (or removes it if empty).
     *
     * @param components the components compound
     * @return this builder
     */
    public RItemBuilder components(@NotNull RNbtCompound components) {
        RNbtCompound safeComponents = Objects.requireNonNull(components, "components");
        data = safeComponents.isEmpty() ? RItemFields.COMPONENTS.remove(data) : RItemFields.COMPONENTS.write(data, safeComponents);
        return this;
    }

    /**
     * Sets the custom data compound (or removes it if empty).
     *
     * @param customData the custom data compound
     * @return this builder
     */
    public RItemBuilder customData(@NotNull RNbtCompound customData) {
        RNbtCompound safeCustomData = Objects.requireNonNull(customData, "customData");
        data = safeCustomData.isEmpty() ? RItemFields.CUSTOM_DATA.remove(data) : RItemFields.CUSTOM_DATA.write(data, safeCustomData);
        return this;
    }

    /**
     * Adds a custom data entry.
     *
     * @param key   the key
     * @param value the NBT value
     * @return this builder
     */
    public RItemBuilder custom(@NotNull String key, @NotNull RNbtValue value) {
        customData(RItemFields.CUSTOM_DATA.read(data).orElse(RNbtCompound.empty()).put(key, value));
        return this;
    }

    /**
     * Adds an attachment value.
     *
     * @param <T>   the attachment value type
     * @param key   the attachment key
     * @param value the value
     * @return this builder
     */
    public <T> RItemBuilder attachment(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        return RItemAttachments.with(this, key, value);
    }

    /**
     * Removes an attachment.
     *
     * @param key the attachment key
     * @return this builder
     */
    public RItemBuilder removeAttachment(@NotNull RAttachmentKey<?> key) {
        return RItemAttachments.without(this, key);
    }

    /**
     * Sets the durability/damage value (or removes it if 0).
     *
     * @param durability the durability
     * @return this builder
     */
    public RItemBuilder durability(int durability) {
        return durability == 0 ? remove(RItemFields.DURABILITY) : set(RItemFields.DURABILITY, Integer.valueOf(durability));
    }

    /**
     * Sets the unbreakable flag (or removes it if false).
     *
     * @param unbreakable the unbreakable flag
     * @return this builder
     */
    public RItemBuilder unbreakable(boolean unbreakable) {
        return unbreakable ? set(RItemFields.UNBREAKABLE, Boolean.TRUE) : remove(RItemFields.UNBREAKABLE);
    }

    /**
     * Sets the custom model data from a primitive int.
     *
     * @param modelData the model data
     * @return this builder
     */
    public RItemBuilder customModelData(int modelData) {
        return customModelData(Integer.valueOf(modelData));
    }

    /**
     * Sets or removes the custom model data.
     *
     * @param modelData the model data, or null to remove
     * @return this builder
     */
    public RItemBuilder customModelData(@Nullable Integer modelData) {
        return modelData == null ? remove(RItemFields.CUSTOM_MODEL_DATA) : set(RItemFields.CUSTOM_MODEL_DATA, modelData);
    }

    /**
     * Sets the repair cost from a primitive int (0 removes it).
     *
     * @param repairCost the repair cost
     * @return this builder
     */
    public RItemBuilder repairCost(int repairCost) {
        return repairCost == 0 ? remove(RItemFields.REPAIR_COST) : set(RItemFields.REPAIR_COST, Integer.valueOf(repairCost));
    }

    /**
     * Sets or removes the repair cost.
     *
     * @param repairCost the repair cost, or null to remove
     * @return this builder
     */
    public RItemBuilder repairCost(@Nullable Integer repairCost) {
        return repairCost == null ? remove(RItemFields.REPAIR_COST) : repairCost(repairCost.intValue());
    }

    /**
     * Sets or removes the enchantment glint override.
     *
     * @param enchantmentGlintOverride the override, or null to remove
     * @return this builder
     */
    public RItemBuilder enchantmentGlintOverride(@Nullable Boolean enchantmentGlintOverride) {
        return enchantmentGlintOverride == null
            ? remove(RItemFields.ENCHANTMENT_GLINT_OVERRIDE)
            : set(RItemFields.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride);
    }

    /**
     * Builds the final {@link RItem}.
     *
     * @return the constructed item
     * @throws IllegalStateException if the type ref has not been set
     */
    public RItem build() {
        if (typeRef == null) {
            throw new IllegalStateException("Type ref must be set");
        }
        RItem result = RItemFactory.tryCreate(typeRef.key(), amount, data);
        return result != null ? result : new SimpleRItem(typeRef, amount, data);
    }
}
