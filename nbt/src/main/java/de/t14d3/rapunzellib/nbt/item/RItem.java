package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.attachments.RAttachmentKey;
import de.t14d3.rapunzellib.attachments.AttachmentStorageSupport;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtField;
import de.t14d3.rapunzellib.nbt.RNbtPath;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface RItem {

    @NotNull RRegistryRef<RItemType> typeRef();

    default @NotNull RKey typeKey() {
        return typeRef().key();
    }

    int amount();

    @NotNull RNbtCompound data();

    default @NotNull String material() {
        return typeKey().asString();
    }

    default @NotNull Optional<RItemType> type() {
        try {
            return typeRef().find();
        } catch (IllegalStateException ignored) {
            return Rapunzel.itemTypes().find(typeKey());
        }
    }

    default @NotNull RItemType requireType() {
        try {
            return typeRef().require();
        } catch (IllegalStateException ignored) {
            return Rapunzel.itemTypes().require(typeKey());
        }
    }

    default @NotNull Optional<Component> name() {
        return get(RItemFields.NAME);
    }

    default @NotNull List<Component> lore() {
        return get(RItemFields.LORE).orElse(List.of());
    }

    default int durability() {
        return get(RItemFields.DURABILITY).orElse(0);
    }

    default boolean unbreakable() {
        return get(RItemFields.UNBREAKABLE).orElse(Boolean.FALSE);
    }

    default @NotNull Optional<Integer> customModelData() {
        return get(RItemFields.CUSTOM_MODEL_DATA);
    }

    default @NotNull Optional<Integer> repairCost() {
        return get(RItemFields.REPAIR_COST);
    }

    default @NotNull Optional<Boolean> enchantmentGlintOverride() {
        return get(RItemFields.ENCHANTMENT_GLINT_OVERRIDE);
    }

    default @NotNull RNbtCompound components() {
        return get(RItemFields.COMPONENTS).orElse(RNbtCompound.empty());
    }

    default @NotNull RNbtCompound customData() {
        return get(RItemFields.CUSTOM_DATA).orElse(RNbtCompound.empty());
    }

    default <T> @NotNull Optional<T> get(@NotNull RNbtField<T> field) {
        return data().get(Objects.requireNonNull(field, "field"));
    }

    default <T> @NotNull Optional<T> get(@NotNull RNbtPath<T> path) {
        return data().get(Objects.requireNonNull(path, "path"));
    }

    default @NotNull Optional<RNbtValue> custom(@NotNull String key) {
        return customData().get(Objects.requireNonNull(key, "key"));
    }

    default boolean supportsAttachment(@NotNull RAttachmentKey<?> key) {
        return attachmentSupport().supports(Objects.requireNonNull(key, "key").scope());
    }

    default @NotNull AttachmentStorageSupport attachmentSupport() {
        return RItemAttachments.support();
    }

    default <T> @NotNull Optional<T> attachment(@NotNull RAttachmentKey<T> key) {
        return RItemAttachments.get(this, key);
    }

    default @NotNull RItem withTypeRef(@NotNull RRegistryRef<RItemType> typeRef) {
        return withTypeKey(Objects.requireNonNull(typeRef, "typeRef").key());
    }

    @NotNull RItem withTypeKey(@NotNull RKey typeKey);

    @NotNull RItem withAmount(int amount);

    @NotNull RItem withData(@NotNull RNbtCompound data);

    default <T> @NotNull RItem with(@NotNull RNbtField<T> field, @NotNull T value) {
        return withData(Objects.requireNonNull(field, "field").write(data(), value));
    }

    default <T> @NotNull RItem with(@NotNull RNbtPath<T> path, @NotNull T value) {
        return withData(Objects.requireNonNull(path, "path").write(data(), value));
    }

    default @NotNull RItem without(@NotNull RNbtField<?> field) {
        return withData(Objects.requireNonNull(field, "field").remove(data()));
    }

    default @NotNull RItem without(@NotNull RNbtPath<?> path) {
        return withData(Objects.requireNonNull(path, "path").remove(data()));
    }

    default @NotNull RItem withName(@Nullable Component name) {
        return name == null ? without(RItemFields.NAME) : with(RItemFields.NAME, name);
    }

    default @NotNull RItem withLore(@NotNull List<Component> lore) {
        List<Component> lines = List.copyOf(Objects.requireNonNull(lore, "lore"));
        return lines.isEmpty() ? without(RItemFields.LORE) : with(RItemFields.LORE, lines);
    }

    default @NotNull RItem withDurability(int durability) {
        return durability == 0 ? without(RItemFields.DURABILITY) : with(RItemFields.DURABILITY, Integer.valueOf(durability));
    }

    default @NotNull RItem withUnbreakable(boolean unbreakable) {
        return unbreakable ? with(RItemFields.UNBREAKABLE, Boolean.TRUE) : without(RItemFields.UNBREAKABLE);
    }

    default @NotNull RItem withCustomModelData(int modelData) {
        return with(RItemFields.CUSTOM_MODEL_DATA, Integer.valueOf(modelData));
    }

    default @NotNull RItem withCustomModelData(@Nullable Integer modelData) {
        return modelData == null ? without(RItemFields.CUSTOM_MODEL_DATA) : withCustomModelData(modelData.intValue());
    }

    default @NotNull RItem withoutCustomModelData() {
        return without(RItemFields.CUSTOM_MODEL_DATA);
    }

    default @NotNull RItem withRepairCost(int repairCost) {
        return repairCost == 0 ? without(RItemFields.REPAIR_COST) : with(RItemFields.REPAIR_COST, Integer.valueOf(repairCost));
    }

    default @NotNull RItem withRepairCost(@Nullable Integer repairCost) {
        return repairCost == null ? without(RItemFields.REPAIR_COST) : withRepairCost(repairCost.intValue());
    }

    default @NotNull RItem withoutRepairCost() {
        return without(RItemFields.REPAIR_COST);
    }

    default @NotNull RItem withEnchantmentGlintOverride(@Nullable Boolean enchantmentGlintOverride) {
        return enchantmentGlintOverride == null
            ? without(RItemFields.ENCHANTMENT_GLINT_OVERRIDE)
            : with(RItemFields.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride);
    }

    default @NotNull RItem withoutEnchantmentGlintOverride() {
        return without(RItemFields.ENCHANTMENT_GLINT_OVERRIDE);
    }

    default @NotNull RItem withComponents(@NotNull RNbtCompound components) {
        RNbtCompound safeComponents = Objects.requireNonNull(components, "components");
        return safeComponents.isEmpty() ? without(RItemFields.COMPONENTS) : with(RItemFields.COMPONENTS, safeComponents);
    }

    default @NotNull RItem withCustomData(@NotNull RNbtCompound customData) {
        RNbtCompound safeCustomData = Objects.requireNonNull(customData, "customData");
        return safeCustomData.isEmpty() ? without(RItemFields.CUSTOM_DATA) : with(RItemFields.CUSTOM_DATA, safeCustomData);
    }

    default @NotNull RItem withCustom(@NotNull String key, @NotNull RNbtValue value) {
        return withCustomData(customData().put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value")));
    }

    default @NotNull RItem withoutCustom(@NotNull String key) {
        return withCustomData(customData().remove(Objects.requireNonNull(key, "key")));
    }

    default <T> @NotNull RItem withAttachment(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        return RItemAttachments.with(this, key, value);
    }

    default @NotNull RItem withoutAttachment(@NotNull RAttachmentKey<?> key) {
        return RItemAttachments.without(this, key);
    }

    static @NotNull RItemBuilder builder() {
        return new RItemBuilder();
    }

    static @NotNull RItem of(@NotNull RKey typeKey) {
        return builder().typeKey(typeKey).build();
    }

    static @NotNull RItem of(@NotNull RRegistryRef<RItemType> typeRef) {
        return builder().typeRef(typeRef).build();
    }

    static @NotNull RItem of(@NotNull RKey typeKey, int amount) {
        return builder().typeKey(typeKey).amount(amount).build();
    }

    static @NotNull RItem of(@NotNull RRegistryRef<RItemType> typeRef, int amount) {
        return builder().typeRef(typeRef).amount(amount).build();
    }

    static @NotNull RItem of(@NotNull String material) {
        return builder().material(material).build();
    }

    static @NotNull RItem of(@NotNull String material, int amount) {
        return builder().material(material).amount(amount).build();
    }
}
