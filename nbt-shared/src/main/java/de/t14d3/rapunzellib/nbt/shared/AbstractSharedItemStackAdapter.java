package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.NativeRItem;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.nbt.item.RItemBuilder;
import de.t14d3.rapunzellib.nbt.item.RItemFields;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryHandles;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractSharedItemStackAdapter implements ItemStackAdapter<ItemStack> {
    private final @NotNull PlatformId platformId;

    protected AbstractSharedItemStackAdapter(@NotNull PlatformId platformId) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    @Override
    public final @NotNull RItem snapshot(@NotNull ItemStack nativeItem) {
        ItemStack copy = nativeItem.copy();
        return NativeRItem.of(
            platformId,
            copy,
            toShared(copy),
            this::updateNativeShared
        );
    }

    @Override
    public final @NotNull ItemStack create(@NotNull RItem item) {
        if (item instanceof RNative nativeItem) {
            ItemStack handle = nativeItem.tryHandle(ItemStack.class).orElse(null);
            if (handle != null) {
                return handle.copy();
            }
        }
        return createNativeShared(item);
    }

    @Override
    public final @NotNull ItemStack apply(@NotNull ItemStack nativeItem, @NotNull RItem item) {
        return updateNativeShared(nativeItem, item);
    }

    @Override
    public final boolean supports(@Nullable Object object) {
        return object instanceof ItemStack;
    }

    protected @NotNull RItem toShared(@NotNull ItemStack nativeItem) {
        net.minecraft.network.chat.Component customName = nativeItem.get(DataComponents.CUSTOM_NAME);
        ItemLore loreComponent = nativeItem.get(DataComponents.LORE);
        CustomData customData = nativeItem.get(DataComponents.CUSTOM_DATA);
        CustomModelData customModelData = nativeItem.get(DataComponents.CUSTOM_MODEL_DATA);
        Integer repairCost = nativeItem.get(DataComponents.REPAIR_COST);
        Boolean enchantmentGlintOverride = nativeItem.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);

        Component name = customName != null ? SharedAdventureComponentCodec.toAdventure(customName) : null;
        List<Component> lore = loreComponent == null || loreComponent.lines().isEmpty()
            ? List.of()
            : loreComponent.lines().stream().map(SharedAdventureComponentCodec::toAdventure).toList();
        RNbtCompound sharedCustomData = customData == null ? RNbtCompound.empty() : SharedNbtIoSupport.toTree(customData.copyTag());
        Integer modelData = readCustomModelData(customModelData);
        int damage = nativeItem.getDamageValue();

        RItemBuilder builder = RItem.builder()
            .typeRef(RItemType.ref(BuiltInRegistries.ITEM.getKey(nativeItem.getItem()).toString()))
            .amount(nativeItem.getCount());

        if (name != null) {
            builder.set(RItemFields.NAME, name);
        }
        if (!lore.isEmpty()) {
            builder.set(RItemFields.LORE, lore);
        }
        if (!sharedCustomData.isEmpty()) {
            builder.set(RItemFields.CUSTOM_DATA, sharedCustomData);
        }
        if (damage > 0) {
            builder.set(RItemFields.DAMAGE, damage);
        }
        if (nativeItem.has(DataComponents.UNBREAKABLE)) {
            builder.set(RItemFields.UNBREAKABLE, Boolean.TRUE);
        }
        if (modelData != null) {
            builder.set(RItemFields.CUSTOM_MODEL_DATA, modelData);
        }
        if (repairCost != null && repairCost != 0) {
            builder.set(RItemFields.REPAIR_COST, repairCost);
        }
        if (enchantmentGlintOverride != null) {
            builder.set(RItemFields.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride);
        }

        return builder.build();
    }

    protected @NotNull ItemStack createNativeShared(@NotNull RItem item) {
        return applySharedState(new ItemStack(resolveItem(item.typeRef()), item.amount()), item);
    }

    protected @NotNull ItemStack updateNativeShared(@NotNull ItemStack currentHandle, @NotNull RItem updatedItem) {
        Item resolvedItem = resolveItem(updatedItem.typeRef());
        ItemStack working = currentHandle.getItem() == resolvedItem
            ? currentHandle.copy()
            : createNativeShared(updatedItem);
        return applySharedState(working, updatedItem);
    }

    protected @NotNull Item resolveItem(@NotNull RKey typeKey) {
        return resolveItem(RItemType.ref(typeKey));
    }

    protected @NotNull Item resolveItem(@NotNull RRegistryRef<RItemType> typeRef) {
        Item resolved = RRegistryHandles.find(typeRef, Item.class).orElse(null);
        if (resolved != null && resolved != Items.AIR) {
            return resolved;
        }
        RKey typeKey = typeRef.key();
        Identifier location = Identifier.tryParse(typeKey.asString());
        if (location == null) {
            location = Identifier.withDefaultNamespace(typeKey.path());
        }
        return BuiltInRegistries.ITEM.getValue(location);
    }

    protected @NotNull ItemStack applySharedState(@NotNull ItemStack stack, @NotNull RItem item) {
        stack.setCount(item.amount());

        item.get(RItemFields.NAME).ifPresentOrElse(
            name -> stack.set(DataComponents.CUSTOM_NAME, SharedAdventureComponentCodec.toNative(name)),
            () -> stack.remove(DataComponents.CUSTOM_NAME)
        );

        List<Component> lore = item.get(RItemFields.LORE).orElse(List.of());
        if (lore.isEmpty()) {
            stack.remove(DataComponents.LORE);
        } else {
            List<net.minecraft.network.chat.Component> lines = new ArrayList<>(lore.size());
            for (Component line : lore) {
                lines.add(SharedAdventureComponentCodec.toNative(line));
            }
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }

        RNbtCompound customData = item.get(RItemFields.CUSTOM_DATA).orElse(RNbtCompound.empty());
        if (customData.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(SharedNbtIoSupport.fromTree(customData)));
        }

        stack.setDamageValue(Math.max(item.get(RItemFields.DAMAGE).orElse(0), 0));

        if (item.get(RItemFields.UNBREAKABLE).orElse(Boolean.FALSE)) {
            stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        } else {
            stack.remove(DataComponents.UNBREAKABLE);
        }

        item.get(RItemFields.CUSTOM_MODEL_DATA).ifPresentOrElse(
            modelData -> stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(modelData))),
            () -> stack.remove(DataComponents.CUSTOM_MODEL_DATA)
        );

        item.get(RItemFields.REPAIR_COST).ifPresentOrElse(
            repairCost -> stack.set(DataComponents.REPAIR_COST, Math.max(repairCost, 0)),
            () -> stack.remove(DataComponents.REPAIR_COST)
        );

        item.get(RItemFields.ENCHANTMENT_GLINT_OVERRIDE).ifPresentOrElse(
            enchantmentGlintOverride -> stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride),
            () -> stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)
        );

        return stack;
    }

    protected @Nullable Integer readCustomModelData(@Nullable CustomModelData customModelData) {
        if (customModelData == null || customModelData.colors().isEmpty()) {
            return null;
        }
        return customModelData.colors().getFirst();
    }
}
