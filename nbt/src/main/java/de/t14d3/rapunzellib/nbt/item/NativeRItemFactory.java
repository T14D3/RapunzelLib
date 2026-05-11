package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NativeRItemFactory {
    @NotNull NativeRItem<?> create(@NotNull RKey typeKey, int amount, @NotNull RNbtCompound data);
}
