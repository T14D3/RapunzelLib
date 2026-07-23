package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, loosely-typed snapshot of an ItemStack held by a bot.
 *
 * <p>This is the wire representation that crosses the bot RPC boundary. It is
 * deliberately kept independent of RapunzelLib's {@code nbt} family so the
 * livetest module does not pull a cross-feature dependency. The fields are:</p>
 *
 * <ul>
 *   <li>{@code id} - the raw Minecraft item registry id as the bot client sees
 *       it. Useful for equality checks and slot predicates. Special value
 *       {@code -1} denotes an empty slot.</li>
 *   <li>{@code amount} - the stack size. {@code 0} for an empty slot.</li>
 *   <li>{@code componentsJson} - a non-typed, plain-JSON serialization of the
 *       item's data component patch when the underlying transport is the TCP
 *       bot protocol, or {@code null} if the bot client did not surface any.
 *       The exact shape of this JSON is <em>unspecified</em> and may change
 *       between Minecraft versions; tests should only treat it as an opaque
 *       payload or as a string match target.</li>
 * </ul>
 *
 * <p>Callers should prefer the convenience predicates ({@link #isEmpty()},
 * {@link #hasId(int)}, {@link #amountAtLeast(int)}) over manual field
 * inspection.</p>
 *
 * @see BotInventory
 */
public record BotItemStack(int id, int amount, @Nullable String componentsJson) {

    /** Sentinel returned by queries for empty slots. */
    public static final BotItemStack EMPTY = new BotItemStack(-1, 0, null);

    public BotItemStack {
        if (id < 0 && amount != 0) {
            // An item with id < 0 must always have amount 0; normalize.
            amount = 0;
        }
    }

    /**
     * Constructs an item stack with no component payload.
     *
     * @param id     the raw item registry id, or {@code -1} for empty
     * @param amount the stack size (must be 0 if {@code id == -1})
     */
    public BotItemStack(int id, int amount) {
        this(id, amount, null);
    }

    /**
     * @return {@code true} if this slot is empty (no item present)
     */
    public boolean isEmpty() {
        return id < 0 || amount <= 0;
    }

    /**
     * @param itemId the raw item id to test for
     * @return {@code true} if this stack's id matches and the slot is non-empty
     */
    public boolean hasId(int itemId) {
        return !isEmpty() && id == itemId;
    }

    /**
     * @param atLeast the minimum quantity required
     * @return {@code true} if this stack holds at least {@code atLeast} items
     */
    public boolean amountAtLeast(int atLeast) {
        return !isEmpty() && amount >= atLeast;
    }

    /**
     * @return this stack's component payload, if any was carried over the wire
     */
    public @NotNull Optional<String> components() {
        return Optional.ofNullable(componentsJson);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotItemStack that)) return false;
        // Empty slots are equal regardless of componentsJson
        if (isEmpty() && that.isEmpty()) return true;
        return id == that.id
                && amount == that.amount
                && Objects.equals(componentsJson, that.componentsJson);
    }

    @Override
    public int hashCode() {
        if (isEmpty()) return BotItemStack.class.hashCode();
        return Objects.hash(id, amount, componentsJson);
    }
}
