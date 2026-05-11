package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface NativeRItemAccessor<H> {
    @NotNull RRegistryRef<RItemType> typeRef(@NotNull H handle);

    void setTypeKey(@NotNull H handle, @NotNull RRegistryRef<RItemType> typeRef);

    int amount(@NotNull H handle);

    void setAmount(@NotNull H handle, int amount);

    @NotNull RNbtCompound data(@NotNull H handle);

    void setData(@NotNull H handle, @NotNull RNbtCompound data);

    @Nullable Component name(@NotNull H handle);

    void setName(@NotNull H handle, @Nullable Component name);

    @NotNull List<Component> lore(@NotNull H handle);

    void setLore(@NotNull H handle, @NotNull List<Component> lore);

    int damage(@NotNull H handle);

    void setDamage(@NotNull H handle, int damage);

    boolean unbreakable(@NotNull H handle);

    void setUnbreakable(@NotNull H handle, boolean unbreakable);

    @Nullable Integer customModelData(@NotNull H handle);

    void setCustomModelData(@NotNull H handle, @Nullable Integer modelData);

    int repairCost(@NotNull H handle);

    void setRepairCost(@NotNull H handle, int repairCost);

    @Nullable Boolean enchantmentGlintOverride(@NotNull H handle);

    void setEnchantmentGlintOverride(@NotNull H handle, @Nullable Boolean override);

    boolean isEmpty(@NotNull H handle);

    int count(@NotNull H handle);

    int maxStackSize(@NotNull H handle);

    boolean isSimilar(@NotNull H handle, @NotNull RItem other);

    @NotNull H createHandle(@NotNull RRegistryRef<RItemType> typeRef, int amount);
}
