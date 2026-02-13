package de.t14d3.rapunzellib.objects.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RBlockData extends RNative {
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

    @NotNull String asString();

    static @NotNull Optional<RBlockData> wrap(@NotNull Object nativeBlockData) {
        return Rapunzel.blocks().wrapData(nativeBlockData);
    }

    static @NotNull Optional<RBlockData> parse(@NotNull String value) {
        return Rapunzel.blocks().parseData(value);
    }
}
