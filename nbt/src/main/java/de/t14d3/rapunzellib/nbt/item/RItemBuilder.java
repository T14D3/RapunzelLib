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

public class RItemBuilder {
    RRegistryRef<RItemType> typeRef;
    int amount = 1;
    RNbtCompound data = RNbtCompound.empty();

    public RItemBuilder typeRef(@NotNull RRegistryRef<RItemType> typeRef) {
        this.typeRef = Objects.requireNonNull(typeRef, "typeRef");
        return this;
    }

    public RItemBuilder typeKey(@NotNull RKey typeKey) {
        return typeRef(RItemType.ref(typeKey));
    }

    public RItemBuilder typeKey(@NotNull String typeKey) {
        return typeKey(RKey.of(typeKey));
    }

    public RItemBuilder material(@NotNull String material) {
        return typeKey(material);
    }

    public RItemBuilder material(@NotNull RKey material) {
        return typeKey(material);
    }

    public RItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    public RItemBuilder data(@NotNull RNbtCompound data) {
        this.data = Objects.requireNonNull(data, "data");
        return this;
    }

    public <T> RItemBuilder set(@NotNull RNbtField<T> field, @NotNull T value) {
        data = Objects.requireNonNull(field, "field").write(data, value);
        return this;
    }

    public <T> RItemBuilder set(@NotNull RNbtPath<T> path, @NotNull T value) {
        data = Objects.requireNonNull(path, "path").write(data, value);
        return this;
    }

    public RItemBuilder remove(@NotNull RNbtField<?> field) {
        data = Objects.requireNonNull(field, "field").remove(data);
        return this;
    }

    public RItemBuilder remove(@NotNull RNbtPath<?> path) {
        data = Objects.requireNonNull(path, "path").remove(data);
        return this;
    }

    public RItemBuilder name(@Nullable Component name) {
        return name == null ? remove(RItemFields.NAME) : set(RItemFields.NAME, name);
    }

    public RItemBuilder lore(@NotNull List<Component> lore) {
        List<Component> lines = new ArrayList<>(Objects.requireNonNull(lore, "lore"));
        return lines.isEmpty() ? remove(RItemFields.LORE) : set(RItemFields.LORE, List.copyOf(lines));
    }

    public RItemBuilder lore(@NotNull Component... lines) {
        return lore(Arrays.asList(lines));
    }

    public RItemBuilder addLore(@NotNull Component line) {
        ArrayList<Component> lines = new ArrayList<>(RItemFields.LORE.read(data).orElse(List.of()));
        lines.add(Objects.requireNonNull(line, "line"));
        data = RItemFields.LORE.write(data, List.copyOf(lines));
        return this;
    }

    public RItemBuilder components(@NotNull RNbtCompound components) {
        RNbtCompound safeComponents = Objects.requireNonNull(components, "components");
        data = safeComponents.isEmpty() ? RItemFields.COMPONENTS.remove(data) : RItemFields.COMPONENTS.write(data, safeComponents);
        return this;
    }

    public RItemBuilder customData(@NotNull RNbtCompound customData) {
        RNbtCompound safeCustomData = Objects.requireNonNull(customData, "customData");
        data = safeCustomData.isEmpty() ? RItemFields.CUSTOM_DATA.remove(data) : RItemFields.CUSTOM_DATA.write(data, safeCustomData);
        return this;
    }

    public RItemBuilder custom(@NotNull String key, @NotNull RNbtValue value) {
        customData(RItemFields.CUSTOM_DATA.read(data).orElse(RNbtCompound.empty()).put(key, value));
        return this;
    }

    public <T> RItemBuilder attachment(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        return RItemAttachments.with(this, key, value);
    }

    public RItemBuilder removeAttachment(@NotNull RAttachmentKey<?> key) {
        return RItemAttachments.without(this, key);
    }

    public RItemBuilder durability(int durability) {
        return durability == 0 ? remove(RItemFields.DURABILITY) : set(RItemFields.DURABILITY, Integer.valueOf(durability));
    }

    public RItemBuilder unbreakable(boolean unbreakable) {
        return unbreakable ? set(RItemFields.UNBREAKABLE, Boolean.TRUE) : remove(RItemFields.UNBREAKABLE);
    }

    public RItemBuilder customModelData(int modelData) {
        return customModelData(Integer.valueOf(modelData));
    }

    public RItemBuilder customModelData(@Nullable Integer modelData) {
        return modelData == null ? remove(RItemFields.CUSTOM_MODEL_DATA) : set(RItemFields.CUSTOM_MODEL_DATA, modelData);
    }

    public RItemBuilder repairCost(int repairCost) {
        return repairCost == 0 ? remove(RItemFields.REPAIR_COST) : set(RItemFields.REPAIR_COST, Integer.valueOf(repairCost));
    }

    public RItemBuilder repairCost(@Nullable Integer repairCost) {
        return repairCost == null ? remove(RItemFields.REPAIR_COST) : repairCost(repairCost.intValue());
    }

    public RItemBuilder enchantmentGlintOverride(@Nullable Boolean enchantmentGlintOverride) {
        return enchantmentGlintOverride == null
            ? remove(RItemFields.ENCHANTMENT_GLINT_OVERRIDE)
            : set(RItemFields.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride);
    }

    public RItem build() {
        if (typeRef == null) {
            throw new IllegalStateException("Type ref must be set");
        }
        return new SimpleRItem(typeRef, amount, data);
    }
}
