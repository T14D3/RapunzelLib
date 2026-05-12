package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.attachments.RAttachmentKey;
import de.t14d3.rapunzellib.attachments.AttachmentStorageSupport;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.RNbtPath;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Cross-platform abstraction for a Minecraft item stack.
 * <p>
 * Provides read and write access to item properties (type, amount, name, lore, durability, etc.)
 * and NBT data through a unified interface that works across different Minecraft server platforms.</p>
 */
public interface RItem {

    /**
     * Returns the item type reference.
     *
     * @return the type reference
     */
    @NotNull RRegistryRef<RItemType> typeRef();

    /**
     * Returns the item type key (shortcut for {@code typeRef().key()}).
     *
     * @return the type key
     */
    default @NotNull RKey typeKey() {
        return typeRef().key();
    }

    /**
     * Returns the amount of items in this stack.
     *
     * @return the amount
     */
    int amount();

    /**
     * Returns the NBT data compound for this item.
     *
     * @return the data compound
     */
    @NotNull RNbtCompound data();

    /**
     * Returns the full material identifier string.
     *
     * @return the material string
     */
    default @NotNull String material() {
        return typeKey().asString();
    }

    /**
     * Looks up the registered {@link RItemType} for this item.
     *
     * @return an Optional containing the item type, or empty if not registered
     */
    default @NotNull Optional<RItemType> type() {
        try {
            return typeRef().find();
        } catch (IllegalStateException ignored) {
            return Rapunzel.itemTypes().find(typeKey());
        }
    }

    /**
     * Requires the registered {@link RItemType} for this item.
     *
     * @return the item type (never null)
     * @throws IllegalStateException if the type is not registered
     */
    default @NotNull RItemType requireType() {
        try {
            return typeRef().require();
        } catch (IllegalStateException ignored) {
            return Rapunzel.itemTypes().require(typeKey());
        }
    }

    /**
     * Returns the custom name component, if set.
     *
     * @return an Optional containing the name, or empty
     */
    default @NotNull Optional<Component> name() {
        return get(RItemFields.NAME);
    }

    /**
     * Returns the lore lines, or an empty list if not set.
     *
     * @return the lore list
     */
    default @NotNull List<Component> lore() {
        return get(RItemFields.LORE).orElse(List.of());
    }

    /**
     * Returns the durability/damage value, or 0 if not set.
     *
     * @return the durability
     */
    default int durability() {
        return get(RItemFields.DURABILITY).orElse(0);
    }

    /**
     * Whether this item is unbreakable.
     *
     * @return true if unbreakable
     */
    default boolean unbreakable() {
        return get(RItemFields.UNBREAKABLE).orElse(Boolean.FALSE);
    }

    /**
     * Returns the custom model data, if set.
     *
     * @return an Optional containing the model data, or empty
     */
    default @NotNull Optional<Integer> customModelData() {
        return get(RItemFields.CUSTOM_MODEL_DATA);
    }

    /**
     * Returns the repair cost, if set.
     *
     * @return an Optional containing the repair cost, or empty
     */
    default @NotNull Optional<Integer> repairCost() {
        return get(RItemFields.REPAIR_COST);
    }

    /**
     * Returns the enchantment glint override, if set.
     *
     * @return an Optional containing the override, or empty
     */
    default @NotNull Optional<Boolean> enchantmentGlintOverride() {
        return get(RItemFields.ENCHANTMENT_GLINT_OVERRIDE);
    }

    /**
     * Returns the components compound (may be empty).
     *
     * @return the components compound
     */
    default @NotNull RNbtCompound components() {
        return get(RItemFields.COMPONENTS).orElse(RNbtCompound.empty());
    }

    /**
     * Returns the custom data compound (may be empty).
     *
     * @return the custom data compound
     */
    default @NotNull RNbtCompound customData() {
        return get(RItemFields.CUSTOM_DATA).orElse(RNbtCompound.empty());
    }

    /**
     * Reads a typed field from the item's data compound.
     *
     * @param <T>   the value type
     * @param field the field descriptor
     * @return an Optional containing the decoded value, or empty
     */
    default <T> @NotNull Optional<T> get(@NotNull RNbtField<T> field) {
        return data().get(Objects.requireNonNull(field, "field"));
    }

    /**
     * Reads a typed path from the item's data compound.
     *
     * @param <T>  the value type
     * @param path the path descriptor
     * @return an Optional containing the decoded value, or empty
     */
    default <T> @NotNull Optional<T> get(@NotNull RNbtPath<T> path) {
        return data().get(Objects.requireNonNull(path, "path"));
    }

    /**
     * Looks up a custom data value by key.
     *
     * @param key the custom data key
     * @return an Optional containing the value, or empty
     */
    default @NotNull Optional<RNbtValue> custom(@NotNull String key) {
        return customData().get(Objects.requireNonNull(key, "key"));
    }

    /**
     * Whether this item supports storing the given attachment type.
     *
     * @param key the attachment key
     * @return true if supported
     */
    default boolean supportsAttachment(@NotNull RAttachmentKey<?> key) {
        return attachmentSupport().supports(Objects.requireNonNull(key, "key").scope());
    }

    /**
     * Returns the attachment storage support level for items.
     *
     * @return the storage support
     */
    default @NotNull AttachmentStorageSupport attachmentSupport() {
        return RItemAttachments.support();
    }

    /**
     * Reads an attachment value by key.
     *
     * @param <T> the attachment value type
     * @param key the attachment key
     * @return an Optional containing the value, or empty if not present or unsupported
     */
    default <T> @NotNull Optional<T> attachment(@NotNull RAttachmentKey<T> key) {
        return RItemAttachments.get(this, key);
    }

    /**
     * Returns a copy with the type changed via an {@link RRegistryRef}.
     *
     * @param typeRef the new type reference
     * @return the new item
     */
    default @NotNull RItem withTypeRef(@NotNull RRegistryRef<RItemType> typeRef) {
        return withTypeKey(Objects.requireNonNull(typeRef, "typeRef").key());
    }

    /**
     * Returns a copy with the type key changed.
     *
     * @param typeKey the new type key
     * @return the new item
     */
    @NotNull RItem withTypeKey(@NotNull RKey typeKey);

    /**
     * Returns a copy with the amount changed.
     *
     * @param amount the new amount
     * @return the new item
     */
    @NotNull RItem withAmount(int amount);

    /**
     * Returns a copy with the NBT data replaced.
     *
     * @param data the new data compound
     * @return the new item
     */
    @NotNull RItem withData(@NotNull RNbtCompound data);

    /**
     * Returns the total count of items in this stack.
     *
     * @return the count
     */
    int count();

    /**
     * Returns the maximum stack size for this item type.
     *
     * @return the max stack size
     */
    int maxStackSize();

    /**
     * Whether this item is similar to another (same type and data, ignoring amount).
     *
     * @param other the other item
     * @return true if similar
     */
    boolean isSimilar(@NotNull RItem other);

    /**
     * Returns a copy with the count changed.
     *
     * @param count the new count
     * @return the new item
     */
    @NotNull RItem withCount(int count);

    /**
     * Whether this stack is empty (amount <= 0).
     *
     * @return true if empty
     */
    boolean isEmpty();

    /**
     * Sets the type key on this item (may throw if immutable).
     *
     * @param typeKey the new type key
     */
    void setTypeKey(@NotNull RKey typeKey);

    /**
     * Sets the amount on this item (may throw if immutable).
     *
     * @param amount the new amount
     */
    void setAmount(int amount);

    /**
     * Returns a copy with a typed field value written into the data compound.
     *
     * @param <T>   the value type
     * @param field the field descriptor
     * @param value the value to write
     * @return the new item
     */
    default <T> @NotNull RItem with(@NotNull RNbtField<T> field, @NotNull T value) {
        return withData(Objects.requireNonNull(field, "field").write(data(), value));
    }

    /**
     * Returns a copy with a typed path value written into the data compound.
     *
     * @param <T>   the value type
     * @param path the path descriptor
     * @param value the value to write
     * @return the new item
     */
    default <T> @NotNull RItem with(@NotNull RNbtPath<T> path, @NotNull T value) {
        return withData(Objects.requireNonNull(path, "path").write(data(), value));
    }

    /**
     * Returns a copy with a field removed from the data compound.
     *
     * @param field the field to remove
     * @return the new item
     */
    default @NotNull RItem without(@NotNull RNbtField<?> field) {
        return withData(Objects.requireNonNull(field, "field").remove(data()));
    }

    /**
     * Returns a copy with a path removed from the data compound.
     *
     * @param path the path to remove
     * @return the new item
     */
    default @NotNull RItem without(@NotNull RNbtPath<?> path) {
        return withData(Objects.requireNonNull(path, "path").remove(data()));
    }

    /**
     * Returns a copy with a custom name set (or removed if null).
     *
     * @param name the name component, or null to remove
     * @return the new item
     */
    default @NotNull RItem withName(@Nullable Component name) {
        return name == null ? without(RItemFields.NAME) : with(RItemFields.NAME, name);
    }

    /**
     * Sets the custom name on this item (may throw if immutable).
     *
     * @param name the name component, or null to clear
     */
    default void setName(@Nullable Component name) {
        if (name == null) {
            without(RItemFields.NAME);
        } else {
            with(RItemFields.NAME, name);
        }
    }

    /**
     * Returns a copy with lore lines set (or removed if empty).
     *
     * @param lore the lore lines
     * @return the new item
     */
    default @NotNull RItem withLore(@NotNull List<Component> lore) {
        List<Component> lines = List.copyOf(Objects.requireNonNull(lore, "lore"));
        return lines.isEmpty() ? without(RItemFields.LORE) : with(RItemFields.LORE, lines);
    }

    /**
     * Sets the lore on this item (may throw if immutable).
     *
     * @param lore the lore lines
     */
    default void setLore(@NotNull List<Component> lore) {
        List<Component> lines = List.copyOf(Objects.requireNonNull(lore, "lore"));
        if (lines.isEmpty()) {
            without(RItemFields.LORE);
        } else {
            with(RItemFields.LORE, lines);
        }
    }

    /**
     * Returns a copy with the durability set (or removed if 0).
     *
     * @param durability the durability value
     * @return the new item
     */
    default @NotNull RItem withDurability(int durability) {
        return durability == 0 ? without(RItemFields.DURABILITY) : with(RItemFields.DURABILITY, Integer.valueOf(durability));
    }

    /**
     * Returns a copy with the unbreakable flag set (or removed if false).
     *
     * @param unbreakable the unbreakable flag
     * @return the new item
     */
    default @NotNull RItem withUnbreakable(boolean unbreakable) {
        return unbreakable ? with(RItemFields.UNBREAKABLE, Boolean.TRUE) : without(RItemFields.UNBREAKABLE);
    }

    /**
     * Returns a copy with the custom model data set.
     *
     * @param modelData the model data value
     * @return the new item
     */
    default @NotNull RItem withCustomModelData(int modelData) {
        return with(RItemFields.CUSTOM_MODEL_DATA, Integer.valueOf(modelData));
    }

    /**
     * Returns a copy with the custom model data set or removed if null.
     *
     * @param modelData the model data value, or null to remove
     * @return the new item
     */
    default @NotNull RItem withCustomModelData(@Nullable Integer modelData) {
        return modelData == null ? without(RItemFields.CUSTOM_MODEL_DATA) : withCustomModelData(modelData.intValue());
    }

    /**
     * Returns a copy with the custom model data removed.
     *
     * @return the new item
     */
    default @NotNull RItem withoutCustomModelData() {
        return without(RItemFields.CUSTOM_MODEL_DATA);
    }

    /**
     * Returns a copy with the repair cost set, or removed if 0.
     *
     * @param repairCost the repair cost
     * @return the new item
     */
    default @NotNull RItem withRepairCost(int repairCost) {
        return repairCost == 0 ? without(RItemFields.REPAIR_COST) : with(RItemFields.REPAIR_COST, Integer.valueOf(repairCost));
    }

    /**
     * Returns a copy with the repair cost set or removed if null.
     *
     * @param repairCost the repair cost, or null to remove
     * @return the new item
     */
    default @NotNull RItem withRepairCost(@Nullable Integer repairCost) {
        return repairCost == null ? without(RItemFields.REPAIR_COST) : withRepairCost(repairCost.intValue());
    }

    /**
     * Returns a copy with the repair cost removed.
     *
     * @return the new item
     */
    default @NotNull RItem withoutRepairCost() {
        return without(RItemFields.REPAIR_COST);
    }

    /**
     * Returns a copy with the enchantment glint override set, or removed if null.
     *
     * @param enchantmentGlintOverride the override, or null to remove
     * @return the new item
     */
    default @NotNull RItem withEnchantmentGlintOverride(@Nullable Boolean enchantmentGlintOverride) {
        return enchantmentGlintOverride == null
            ? without(RItemFields.ENCHANTMENT_GLINT_OVERRIDE)
            : with(RItemFields.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride);
    }

    /**
     * Returns a copy with the enchantment glint override removed.
     *
     * @return the new item
     */
    default @NotNull RItem withoutEnchantmentGlintOverride() {
        return without(RItemFields.ENCHANTMENT_GLINT_OVERRIDE);
    }

    /**
     * Returns a copy with the components compound set (or removed if empty).
     *
     * @param components the components compound
     * @return the new item
     */
    default @NotNull RItem withComponents(@NotNull RNbtCompound components) {
        RNbtCompound safeComponents = Objects.requireNonNull(components, "components");
        return safeComponents.isEmpty() ? without(RItemFields.COMPONENTS) : with(RItemFields.COMPONENTS, safeComponents);
    }

    /**
     * Returns a copy with the custom data compound set (or removed if empty).
     *
     * @param customData the custom data compound
     * @return the new item
     */
    default @NotNull RItem withCustomData(@NotNull RNbtCompound customData) {
        RNbtCompound safeCustomData = Objects.requireNonNull(customData, "customData");
        return safeCustomData.isEmpty() ? without(RItemFields.CUSTOM_DATA) : with(RItemFields.CUSTOM_DATA, safeCustomData);
    }

    /**
     * Returns a copy with a custom data entry added.
     *
     * @param key   the custom data key
     * @param value the value
     * @return the new item
     */
    default @NotNull RItem withCustom(@NotNull String key, @NotNull RNbtValue value) {
        return withCustomData(customData().put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value")));
    }

    /**
     * Returns a copy with a custom data entry removed.
     *
     * @param key the custom data key
     * @return the new item
     */
    default @NotNull RItem withoutCustom(@NotNull String key) {
        return withCustomData(customData().remove(Objects.requireNonNull(key, "key")));
    }

    /**
     * Returns a copy with an attachment value added.
     *
     * @param <T>   the attachment value type
     * @param key   the attachment key
     * @param value the value
     * @return the new item
     */
    default <T> @NotNull RItem withAttachment(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        return RItemAttachments.with(this, key, value);
    }

    /**
     * Returns a copy with an attachment removed.
     *
     * @param key the attachment key
     * @return the new item
     */
    default @NotNull RItem withoutAttachment(@NotNull RAttachmentKey<?> key) {
        return RItemAttachments.without(this, key);
    }

    /**
     * Creates a new {@link RItemBuilder} for fluently constructing items.
     *
     * @return a new builder
     */
    static @NotNull RItemBuilder builder() {
        return new RItemBuilder();
    }

    /**
     * Creates a simple item from a type key with amount 1 and empty data.
     *
     * @param typeKey the item type key
     * @return the new item
     */
    static @NotNull RItem of(@NotNull RKey typeKey) {
        RItem result = RItemFactory.tryCreate(typeKey, 1, RNbtCompound.empty());
        return result != null ? result : builder().typeKey(typeKey).build();
    }

    /**
     * Creates a simple item from a type reference with amount 1 and empty data.
     *
     * @param typeRef the item type reference
     * @return the new item
     */
    static @NotNull RItem of(@NotNull RRegistryRef<RItemType> typeRef) {
        RItem result = RItemFactory.tryCreate(typeRef.key(), 1, RNbtCompound.empty());
        return result != null ? result : builder().typeRef(typeRef).build();
    }

    /**
     * Creates a simple item from a type key and amount.
     *
     * @param typeKey the item type key
     * @param amount the stack amount
     * @return the new item
     */
    static @NotNull RItem of(@NotNull RKey typeKey, int amount) {
        RItem result = RItemFactory.tryCreate(typeKey, amount, RNbtCompound.empty());
        return result != null ? result : builder().typeKey(typeKey).amount(amount).build();
    }

    /**
     * Creates a simple item from a type reference and amount.
     *
     * @param typeRef the item type reference
     * @param amount  the stack amount
     * @return the new item
     */
    static @NotNull RItem of(@NotNull RRegistryRef<RItemType> typeRef, int amount) {
        RItem result = RItemFactory.tryCreate(typeRef.key(), amount, RNbtCompound.empty());
        return result != null ? result : builder().typeRef(typeRef).amount(amount).build();
    }

    /**
     * Creates a simple item from a material string with amount 1.
     *
     * @param material the material string
     * @return the new item
     */
    static @NotNull RItem of(@NotNull String material) {
        RItem result = RItemFactory.tryCreate(RKey.of(material), 1, RNbtCompound.empty());
        return result != null ? result : builder().material(material).build();
    }

    /**
     * Creates a simple item from a material string and amount.
     *
     * @param material the material string
     * @param amount   the stack amount
     * @return the new item
     */
    static @NotNull RItem of(@NotNull String material, int amount) {
        RItem result = RItemFactory.tryCreate(RKey.of(material), amount, RNbtCompound.empty());
        return result != null ? result : builder().material(material).amount(amount).build();
    }
}
