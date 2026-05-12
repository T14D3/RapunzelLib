package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Low-level accessor interface for reading and writing properties of a native item stack handle.
 * <p>
 * Platform implementations provide concrete accessors that bridge between the native item stack
 * representation and the Rapunzel NBT item abstraction.</p>
 *
 * @param <H> the native item stack handle type
 */
public interface NativeRItemAccessor<H> {
    /**
     * Returns the type reference of the given handle.
     *
     * @param handle the native handle
     * @return the type reference
     */
    @NotNull RRegistryRef<RItemType> typeRef(@NotNull H handle);

    /**
     * Sets the type on the given handle.
     *
     * @param handle  the native handle
     * @param typeRef the new type reference
     */
    void setTypeKey(@NotNull H handle, @NotNull RRegistryRef<RItemType> typeRef);

    /**
     * Returns the amount of the given handle.
     *
     * @param handle the native handle
     * @return the amount
     */
    int amount(@NotNull H handle);

    /**
     * Sets the amount on the given handle.
     *
     * @param handle the native handle
     * @param amount the new amount
     */
    void setAmount(@NotNull H handle, int amount);

    /**
     * Reads the NBT data from the given handle.
     *
     * @param handle the native handle
     * @return the NBT data compound
     */
    @NotNull RNbtCompound data(@NotNull H handle);

    /**
     * Writes NBT data onto the given handle.
     *
     * @param handle the native handle
     * @param data   the NBT data compound
     */
    void setData(@NotNull H handle, @NotNull RNbtCompound data);

    /**
     * Returns the custom name of the given handle.
     *
     * @param handle the native handle
     * @return the name, or null if not set
     */
    @Nullable Component name(@NotNull H handle);

    /**
     * Sets the custom name on the given handle.
     *
     * @param handle the native handle
     * @param name   the name, or null to clear
     */
    void setName(@NotNull H handle, @Nullable Component name);

    /**
     * Returns the lore of the given handle.
     *
     * @param handle the native handle
     * @return the lore list
     */
    @NotNull List<Component> lore(@NotNull H handle);

    /**
     * Sets the lore on the given handle.
     *
     * @param handle the native handle
     * @param lore   the lore list
     */
    void setLore(@NotNull H handle, @NotNull List<Component> lore);

    /**
     * Returns the damage value of the given handle.
     *
     * @param handle the native handle
     * @return the damage
     */
    int damage(@NotNull H handle);

    /**
     * Sets the damage on the given handle.
     *
     * @param handle the native handle
     * @param damage the damage value
     */
    void setDamage(@NotNull H handle, int damage);

    /**
     * Whether the given handle is unbreakable.
     *
     * @param handle the native handle
     * @return true if unbreakable
     */
    boolean unbreakable(@NotNull H handle);

    /**
     * Sets the unbreakable flag on the given handle.
     *
     * @param handle      the native handle
     * @param unbreakable the unbreakable flag
     */
    void setUnbreakable(@NotNull H handle, boolean unbreakable);

    /**
     * Returns the custom model data of the given handle.
     *
     * @param handle the native handle
     * @return the model data, or null if not set
     */
    @Nullable Integer customModelData(@NotNull H handle);

    /**
     * Sets the custom model data on the given handle.
     *
     * @param handle    the native handle
     * @param modelData the model data, or null to clear
     */
    void setCustomModelData(@NotNull H handle, @Nullable Integer modelData);

    /**
     * Returns the repair cost of the given handle.
     *
     * @param handle the native handle
     * @return the repair cost
     */
    int repairCost(@NotNull H handle);

    /**
     * Sets the repair cost on the given handle.
     *
     * @param handle     the native handle
     * @param repairCost the repair cost
     */
    void setRepairCost(@NotNull H handle, int repairCost);

    /**
     * Returns the enchantment glint override of the given handle.
     *
     * @param handle the native handle
     * @return the override, or null if not set
     */
    @Nullable Boolean enchantmentGlintOverride(@NotNull H handle);

    /**
     * Sets the enchantment glint override on the given handle.
     *
     * @param handle  the native handle
     * @param override the override, or null to clear
     */
    void setEnchantmentGlintOverride(@NotNull H handle, @Nullable Boolean override);

    /**
     * Whether the given handle represents an empty stack.
     *
     * @param handle the native handle
     * @return true if empty
     */
    boolean isEmpty(@NotNull H handle);

    /**
     * Returns the count of items in the given handle.
     *
     * @param handle the native handle
     * @return the count
     */
    int count(@NotNull H handle);

    /**
     * Returns the max stack size of the given handle.
     *
     * @param handle the native handle
     * @return the max stack size
     */
    int maxStackSize(@NotNull H handle);

    /**
     * Whether the given handle is similar to the given item (same type and data).
     *
     * @param handle the native handle
     * @param other  the other item
     * @return true if similar
     */
    boolean isSimilar(@NotNull H handle, @NotNull RItem other);

    /**
     * Creates a new native handle for the given type and amount.
     *
     * @param typeRef the type reference
     * @param amount  the stack amount
     * @return the new handle
     */
    @NotNull H createHandle(@NotNull RRegistryRef<RItemType> typeRef, int amount);
}
