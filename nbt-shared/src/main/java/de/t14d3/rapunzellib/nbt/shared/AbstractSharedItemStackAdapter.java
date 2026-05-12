package de.t14d3.rapunzellib.nbt.shared;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.NativeRItem;
import de.t14d3.rapunzellib.nbt.item.NativeRItemAccessor;
import de.t14d3.rapunzellib.nbt.item.NativeRItemFactory;
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
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
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

/**
 * Abstract base for platform-specific {@link ItemStackAdapter} implementations
 * that bridge between Rapunzel's {@link RItem} and Minecraft's {@link ItemStack}.
 * <p>
 * Implements {@link NativeRItemAccessor} to provide read/write access to all
 * standard item properties via Minecraft's data component system.
 */
public abstract class AbstractSharedItemStackAdapter implements ItemStackAdapter<ItemStack>, NativeRItemAccessor<ItemStack> {
    private final @NotNull PlatformId platformId;

    /**
     * Creates an adapter for the given platform.
     *
     * @param platformId the platform identifier
     */
    protected AbstractSharedItemStackAdapter(@NotNull PlatformId platformId) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    @Override
    public final @NotNull RItem snapshot(@NotNull ItemStack nativeItem) {
        ItemStack copy = nativeItem.copy();
        return createLive(copy);
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

    /**
     * Creates a live {@link NativeRItem} wrapping the given ItemStack handle.
     *
     * @param handle the native ItemStack handle
     * @return the live native RItem
     */
    public final @NotNull NativeRItem<ItemStack> createLive(@NotNull ItemStack handle) {
        return NativeRItem.of(platformId, handle, this);
    }

    /**
     * Creates a factory for producing new NativeRItem instances.
     *
     * @return the factory
     */
    public final @NotNull NativeRItemFactory factory() {
        return (typeKey, amount, data) -> {
            ItemStack stack = createNativeShared(RItem.builder().typeKey(typeKey).amount(amount).data(data).build());
            return createLive(stack);
        };
    }

    // ---- NativeRItemAccessor implementation ----

    @Override
    public @NotNull RRegistryRef<RItemType> typeRef(@NotNull ItemStack handle) {
        return RItemType.ref(BuiltInRegistries.ITEM.getKey(handle.getItem()).toString());
    }

    @Override
    public void setTypeKey(@NotNull ItemStack handle, @NotNull RRegistryRef<RItemType> typeRef) {
        Item resolvedItem = resolveItem(typeRef);
        if (resolvedItem == handle.getItem()) {
            return;
        }
        RItem oldData = toShared(handle);
        // #if VERSION >= 1.21.11
        ItemStack newStack = new ItemStack(resolveItemHolder(typeRef), handle.getCount());
        // #else
        ItemStack newStack = new ItemStack(resolvedItem, handle.getCount());
        // #endif
        applySharedState(newStack, RItem.builder()
            .typeRef(typeRef)
            .amount(oldData.amount())
            .data(oldData.data())
            .build());
        handle.setCount(newStack.getCount());
    }

    @Override
    public int amount(@NotNull ItemStack handle) {
        return handle.getCount();
    }

    @Override
    public void setAmount(@NotNull ItemStack handle, int amount) {
        handle.setCount(amount);
    }

    @Override
    public @NotNull RNbtCompound data(@NotNull ItemStack handle) {
        return toShared(handle).data();
    }

    @Override
    public void setData(@NotNull ItemStack handle, @NotNull RNbtCompound data) {
        RItem temp = RItem.builder()
            .typeRef(RItemType.ref(BuiltInRegistries.ITEM.getKey(handle.getItem()).toString()))
            .amount(handle.getCount())
            .data(data)
            .build();
        applySharedState(handle, temp);
    }

    @Override
    public @Nullable Component name(@NotNull ItemStack handle) {
        net.minecraft.network.chat.Component customName = handle.get(DataComponents.CUSTOM_NAME);
        return customName != null ? SharedAdventureComponentCodec.toAdventure(customName) : null;
    }

    @Override
    public void setName(@NotNull ItemStack handle, @Nullable Component name) {
        if (name != null) {
            handle.set(DataComponents.CUSTOM_NAME, SharedAdventureComponentCodec.toNative(name));
        } else {
            handle.remove(DataComponents.CUSTOM_NAME);
        }
    }

    @Override
    public @NotNull List<Component> lore(@NotNull ItemStack handle) {
        ItemLore loreComponent = handle.get(DataComponents.LORE);
        if (loreComponent == null || loreComponent.lines().isEmpty()) {
            return List.of();
        }
        return loreComponent.lines().stream().map(SharedAdventureComponentCodec::toAdventure).toList();
    }

    @Override
    public void setLore(@NotNull ItemStack handle, @NotNull List<Component> lore) {
        if (lore.isEmpty()) {
            handle.remove(DataComponents.LORE);
        } else {
            List<net.minecraft.network.chat.Component> lines = new ArrayList<>(lore.size());
            for (Component line : lore) {
                lines.add(SharedAdventureComponentCodec.toNative(line));
            }
            handle.set(DataComponents.LORE, new ItemLore(lines));
        }
    }

    @Override
    public int damage(@NotNull ItemStack handle) {
        return handle.getDamageValue();
    }

    @Override
    public void setDamage(@NotNull ItemStack handle, int damage) {
        handle.setDamageValue(Math.max(damage, 0));
    }

    @Override
    public boolean unbreakable(@NotNull ItemStack handle) {
        return handle.has(DataComponents.UNBREAKABLE);
    }

    @Override
    public void setUnbreakable(@NotNull ItemStack handle, boolean unbreakable) {
        if (unbreakable) {
            handle.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        } else {
            handle.remove(DataComponents.UNBREAKABLE);
        }
    }

    @Override
    public @Nullable Integer customModelData(@NotNull ItemStack handle) {
        CustomModelData cmd = handle.get(DataComponents.CUSTOM_MODEL_DATA);
        return readCustomModelData(cmd);
    }

    @Override
    public void setCustomModelData(@NotNull ItemStack handle, @Nullable Integer modelData) {
        if (modelData != null) {
            handle.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(modelData)));
        } else {
            handle.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    @Override
    public int repairCost(@NotNull ItemStack handle) {
        Integer cost = handle.get(DataComponents.REPAIR_COST);
        return cost != null ? cost : 0;
    }

    @Override
    public void setRepairCost(@NotNull ItemStack handle, int repairCost) {
        if (repairCost > 0) {
            handle.set(DataComponents.REPAIR_COST, repairCost);
        } else {
            handle.remove(DataComponents.REPAIR_COST);
        }
    }

    @Override
    public @Nullable Boolean enchantmentGlintOverride(@NotNull ItemStack handle) {
        return handle.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    @Override
    public void setEnchantmentGlintOverride(@NotNull ItemStack handle, @Nullable Boolean override) {
        if (override != null) {
            handle.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, override);
        } else {
            handle.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }

    @Override
    public boolean isEmpty(@NotNull ItemStack handle) {
        return handle.isEmpty();
    }

    @Override
    public int count(@NotNull ItemStack handle) {
        return handle.getCount();
    }

    @Override
    public int maxStackSize(@NotNull ItemStack handle) {
        return handle.getMaxStackSize();
    }

    @Override
    public boolean isSimilar(@NotNull ItemStack handle, @NotNull RItem other) {
        RRegistryRef<RItemType> otherType = other.typeRef();
        if (!typeRef(handle).equals(otherType)) {
            return false;
        }
        return data(handle).equals(other.data());
    }

    @Override
    public @NotNull ItemStack createHandle(@NotNull RRegistryRef<RItemType> typeRef, int amount) {
        // #if VERSION >= 1.21.11
        return new ItemStack(resolveItemHolder(typeRef), amount);
        // #else
        return new ItemStack(resolveItem(typeRef), amount);
        // #endif
    }

    // ---- Existing helper methods ----

    /**
     * Converts a native ItemStack to a shared {@link RItem}.
     *
     * @param nativeItem the native item stack
     * @return the shared RItem
     */
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

    /**
     * Creates a new native ItemStack from a shared RItem.
     *
     * @param item the shared RItem
     * @return the native ItemStack
     */
    protected @NotNull ItemStack createNativeShared(@NotNull RItem item) {
        // #if VERSION >= 1.21.11
        return applySharedState(new ItemStack(resolveItemHolder(item.typeRef()), item.amount()), item);
        // #else
        return applySharedState(new ItemStack(resolveItem(item.typeRef()), item.amount()), item);
        // #endif
    }

    /**
     * Updates an existing native ItemStack to match a shared RItem.
     *
     * @param currentHandle the current native handle
     * @param updatedItem   the updated shared RItem
     * @return the updated native ItemStack
     */
    protected @NotNull ItemStack updateNativeShared(@NotNull ItemStack currentHandle, @NotNull RItem updatedItem) {
        Item resolvedItem = resolveItem(updatedItem.typeRef());
        ItemStack working = currentHandle.getItem() == resolvedItem
            ? currentHandle.copy()
            : createNativeShared(updatedItem);
        return applySharedState(working, updatedItem);
    }

    /**
     * Resolves a Minecraft Item from a type key.
     *
     * @param typeKey the type key
     * @return the resolved Item
     */
    protected @NotNull Item resolveItem(@NotNull RKey typeKey) {
        return resolveItem(RItemType.ref(typeKey));
    }

    /**
     * Resolves a Minecraft Item from a type reference.
     *
     * @param typeRef the type reference
     * @return the resolved Item
     */
    protected @NotNull Item resolveItem(@NotNull RRegistryRef<RItemType> typeRef) {
        Item resolved = RRegistryHandles.find(typeRef, Item.class).orElse(null);
        if (resolved != null && resolved != Items.AIR) {
            return resolved;
        }
        RKey typeKey = typeRef.key();
        // #if VERSION >= 1.21.11
        Identifier location = Identifier.tryParse(typeKey.asString());
        if (location == null) {
            location = Identifier.withDefaultNamespace(typeKey.path());
        }
        // #else
        ResourceLocation location = ResourceLocation.tryParse(typeKey.asString());
        if (location == null) {
            location = ResourceLocation.withDefaultNamespace(typeKey.path());
        }
        // #endif
        return BuiltInRegistries.ITEM.getValue(location);
    }

    // #if VERSION >= 1.21.11
    /**
     * Resolves an Item holder from a type reference (1.21.11+).
     *
     * @param typeRef the type reference
     * @return the Item holder
     */
    protected @NotNull Holder<Item> resolveItemHolder(@NotNull RRegistryRef<RItemType> typeRef) {
        Item resolved = RRegistryHandles.find(typeRef, Item.class).orElse(null);
        if (resolved != null && resolved != Items.AIR) {
            return BuiltInRegistries.ITEM.wrapAsHolder(resolved);
        }

        RKey typeKey = typeRef.key();
        Identifier location = Identifier.tryParse(typeKey.asString());
        if (location == null) {
            location = Identifier.withDefaultNamespace(typeKey.path());
        }
        return BuiltInRegistries.ITEM.get(location).orElseGet(Items.AIR::builtInRegistryHolder);
    }
    // #endif

    /**
     * Applies shared RItem state onto a native ItemStack.
     *
     * @param stack the native ItemStack
     * @param item  the shared RItem
     * @return the modified native ItemStack
     */
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

    /**
     * Reads the custom model data integer from a {@link CustomModelData} component.
     *
     * @param customModelData the component, may be null
     * @return the model data value, or {@code null}
     */
    protected @Nullable Integer readCustomModelData(@Nullable CustomModelData customModelData) {
        if (customModelData == null || customModelData.colors().isEmpty()) {
            return null;
        }
        return customModelData.colors().getFirst();
    }
}
