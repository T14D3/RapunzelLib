package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RContainer;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.snapshot.RBlockSnapshot;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Live server-thread block wrapper.
 *
 * <p>Implementations expose mutable live game state. Use {@link #snapshot()} for
 * immutable async-safe data.</p>
 */
public interface RBlock extends RNative {
    @NotNull RWorld world();

    @NotNull RBlockPos pos();

    @NotNull RRegistryRef<RBlockType> typeRef();

    default @NotNull RKey typeKey() {
        return typeRef().key();
    }

    default @NotNull String typeId() {
        return typeKey().asString();
    }

    default @NotNull Optional<RBlockType> type() {
        try {
            return typeRef().find();
        } catch (IllegalStateException ignored) {
            return Rapunzel.blockTypes().find(typeKey());
        }
    }

    default @NotNull RBlockType requireType() {
        try {
            return typeRef().require();
        } catch (IllegalStateException ignored) {
            return Rapunzel.blockTypes().require(typeKey());
        }
    }

    @NotNull RBlockData data();

    /**
     * Captures immutable block state for async-safe/event-later usage.
     */
    default @NotNull RBlockSnapshot snapshot() {
        return RBlockSnapshot.capture(this);
    }

    default boolean canSetData() {
        return false;
    }

    /**
     * Applies raw block-state mutation to the live block.
     *
     * <p>This is a live server-thread mutation and may trigger the platform's normal
     * block update behavior.</p>
     */
    default boolean setData(@NotNull RBlockData data) {
        throw new UnsupportedOperationException("setData is not supported for " + getClass().getName());
    }

    /**
     * Returns a live container wrapper when the inventory feature is installed.
     * Supported platforms return a strongly typed container surface and may also
     * expose the inventory module's live {@code RInventory} wrapper.
     */
    default @NotNull Optional<RContainer> container() {
        return Optional.empty();
    }

    /**
     * Returns the live container view narrowed to a concrete wrapper type.
     *
     * <p>Consumers can pass the inventory module's {@code RInventory.class} to reuse the
     * shared inventory model without platform-specific casts.</p>
     */
    default <T extends RContainer> @NotNull Optional<T> container(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        return container().filter(type::isInstance).map(type::cast);
    }

    default @NotNull RContainer requireContainer() {
        return container().orElseThrow(() -> new IllegalStateException(
            "Block does not expose a live container for " + getClass().getName()
        ));
    }

    default <T extends RContainer> @NotNull T requireContainer(@NotNull Class<T> type) {
        return container(type).orElseThrow(() -> new IllegalStateException(
            "Container does not expose " + type.getName() + " for " + getClass().getName()
        ));
    }

    static @NotNull Optional<RBlock> wrap(@NotNull Object nativeBlock) {
        return Rapunzel.blocks().wrap(nativeBlock);
    }

    static @NotNull RBlock at(@NotNull RWorld world, @NotNull RBlockPos pos) {
        return Rapunzel.blocks().at(world, pos);
    }
}
