package de.t14d3.rapunzellib.nbt.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.objects.RNative;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PaperItemStackAdapter implements ItemStackAdapter<ItemStack> {
    private final PaperSharedItemStackAdapter bridge = new PaperSharedItemStackAdapter();

    @Override
    public @NotNull RItem snapshot(@NotNull ItemStack nativeItem) {
        net.minecraft.world.item.ItemStack nmsCopy = CraftItemStack.asNMSCopy(nativeItem);
        return bridge.createLive(nmsCopy);
    }

    @Override
    public @NotNull ItemStack create(@NotNull RItem item) {
        if (item instanceof RNative nativeItem) {
            ItemStack bukkitHandle = nativeItem.tryHandle(ItemStack.class).orElse(null);
            if (bukkitHandle != null) {
                return bukkitHandle.clone();
            }

            net.minecraft.world.item.ItemStack mojangHandle = nativeItem.tryHandle(net.minecraft.world.item.ItemStack.class).orElse(null);
            if (mojangHandle != null) {
                return CraftItemStack.asBukkitCopy(mojangHandle.copy());
            }
        }

        return CraftItemStack.asBukkitCopy(bridge.createShared(item));
    }

    @Override
    public @NotNull ItemStack apply(@NotNull ItemStack nativeItem, @NotNull RItem item) {
        return CraftItemStack.asBukkitCopy(bridge.applyShared(CraftItemStack.asNMSCopy(nativeItem), item));
    }

    @Override
    public boolean supports(@Nullable Object object) {
        return object instanceof ItemStack;
    }

    public @NotNull ItemStack createItem(@NotNull String material, int amount, @Nullable Component name, @Nullable List<Component> lore) {
        return create(RItem.builder()
            .material(material)
            .amount(amount)
            .name(name)
            .lore(lore == null ? List.of() : lore)
            .build());
    }

    public @NotNull ItemStack createItem(@NotNull String material, int amount, @Nullable Component name) {
        return createItem(material, amount, name, null);
    }

    public @NotNull ItemStack createItem(@NotNull String material, @Nullable Component name) {
        return createItem(material, 1, name, null);
    }

    public @NotNull ItemStack withLore(@NotNull ItemStack stack, @NotNull List<Component> lore) {
        ItemStack copy = stack.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.lore(new ArrayList<>(lore));
            copy.setItemMeta(meta);
        }
        return copy;
    }

    public @NotNull ItemStack addGlow(@NotNull ItemStack stack) {
        ItemStack copy = stack.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            copy.setItemMeta(meta);
        }
        return copy;
    }
}
