package de.t14d3.rapunzellib.nbt.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.NativeRItem;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.nbt.item.RItemBuilder;
import de.t14d3.rapunzellib.nbt.item.RItemFields;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryHandles;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.List;

public class SpongeItemStackAdapter implements ItemStackAdapter<ItemStack> {
    @Override
    public @NotNull RItem snapshot(@NotNull ItemStack nativeItem) {
        ItemStack copy = nativeItem.copy();
        return NativeRItem.of(
            PlatformId.SPONGE,
            copy,
            this::toShared,
            this::apply
        );
    }

    private @NotNull RItem toShared(@NotNull ItemStack nativeItem) {
        RItemBuilder builder = RItem.builder()
            .typeRef(RItemType.ref(nativeItem.type().key(RegistryTypes.ITEM_TYPE).asString()))
            .amount(nativeItem.quantity())
            .name(nativeItem.get(Keys.CUSTOM_NAME).orElse(null))
            .lore(nativeItem.get(Keys.LORE).orElse(List.of()))
            .durability(nativeItem.get(Keys.ITEM_DURABILITY).orElse(0))
            .unbreakable(nativeItem.get(Keys.IS_UNBREAKABLE).orElse(false))
            .customModelData(nativeItem.get(Keys.CUSTOM_MODEL_DATA).orElse(null));

        nativeItem.get(Keys.REPAIR_COST).ifPresent(value -> builder.set(RItemFields.REPAIR_COST, value));
        nativeItem.get(Keys.ENCHANTMENT_GLINT_OVERRIDE).ifPresent(value -> builder.set(RItemFields.ENCHANTMENT_GLINT_OVERRIDE, value));
        return builder.build();
    }

    @Override
    public @NotNull ItemStack create(@NotNull RItem item) {
        if (item instanceof RNative nativeItem) {
            ItemStack handle = nativeItem.tryHandle(ItemStack.class).orElse(null);
            if (handle != null) {
                return handle.copy();
            }
        }
        return toNativeShared(item);
    }

    @Override
    public @NotNull ItemStack apply(@NotNull ItemStack nativeItem, @NotNull RItem item) {
        return updateNativeShared(nativeItem, item);
    }

    private @NotNull ItemStack toNativeShared(@NotNull RItem item) {
        ItemType itemType = resolveItemType(item.typeRef());
        ItemStack stack = ItemStack.builder().itemType(itemType).quantity(item.amount()).build();
        return applySharedState(stack, item);
    }

    private @NotNull ItemStack updateNativeShared(@NotNull ItemStack currentHandle, @NotNull RItem item) {
        ItemType itemType = resolveItemType(item.typeRef());
        ItemStack stack = currentHandle.type().equals(itemType)
            ? currentHandle.copy()
            : ItemStack.builder().fromItemStack(currentHandle).itemType(itemType).quantity(item.amount()).build();
        return applySharedState(stack, item);
    }

    private @NotNull ItemStack applySharedState(@NotNull ItemStack stack, @NotNull RItem item) {
        stack.setQuantity(item.amount());
        item.name().ifPresentOrElse(
            name -> stack.offer(Keys.CUSTOM_NAME, name),
            () -> stack.remove(Keys.CUSTOM_NAME)
        );
        if (item.lore().isEmpty()) {
            stack.remove(Keys.LORE);
        } else {
            stack.offer(Keys.LORE, item.lore());
        }
        stack.offer(Keys.ITEM_DURABILITY, Math.max(item.durability(), 0));
        stack.offer(Keys.IS_UNBREAKABLE, item.unbreakable());
        item.customModelData().ifPresentOrElse(
            modelData -> stack.offer(Keys.CUSTOM_MODEL_DATA, modelData),
            () -> stack.remove(Keys.CUSTOM_MODEL_DATA)
        );
        item.repairCost().ifPresentOrElse(
            repairCost -> stack.offer(Keys.REPAIR_COST, repairCost),
            () -> stack.remove(Keys.REPAIR_COST)
        );
        item.enchantmentGlintOverride().ifPresentOrElse(
            value -> stack.offer(Keys.ENCHANTMENT_GLINT_OVERRIDE, value),
            () -> stack.remove(Keys.ENCHANTMENT_GLINT_OVERRIDE)
        );
        return stack;
    }

    @Override
    public boolean supports(@Nullable Object object) {
        return object instanceof ItemStack;
    }

    private @NotNull ItemType resolveItemType(@NotNull RRegistryRef<RItemType> typeRef) {
        ItemType resolved = RRegistryHandles.find(typeRef, ItemType.class).orElse(null);
        if (resolved != null && !resolved.equals(ItemTypes.AIR.get())) {
            return resolved;
        }
        return resolveItemType(typeRef.key());
    }

    private @NotNull ItemType resolveItemType(@NotNull RKey typeKey) {
        try {
            return Sponge.server().registry(RegistryTypes.ITEM_TYPE)
                .findValue(org.spongepowered.api.ResourceKey.resolve(typeKey.asString()))
                .orElse(ItemTypes.AIR.get());
        } catch (Exception e) {
            return ItemTypes.AIR.get();
        }
    }
}
