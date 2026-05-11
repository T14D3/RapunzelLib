package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class RItemFactory {
    private RItemFactory() {
    }

    static @Nullable RItem tryCreate(@NotNull RKey typeKey, int amount, @NotNull RNbtCompound data) {
        return Rapunzel.findContext()
            .flatMap(ctx -> ctx.services().find(NativeRItemFactory.class))
            .map(factory -> factory.create(typeKey, amount, data))
            .orElse(null);
    }
}
