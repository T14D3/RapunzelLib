package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

public final class SharedCommandSourceAdapterCore {
    private SharedCommandSourceAdapterCore() {
    }

    public static <T> RCommandSource wrap(
        PlatformId platformId,
        T source,
        BiPredicate<? super T, ? super String> permissionChecker,
        ThrowingFunction<? super T, ?> playerExtractor
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        Objects.requireNonNull(playerExtractor, "playerExtractor");

        Object nativePlayer;
        try {
            nativePlayer = playerExtractor.apply(source);
        } catch (Exception ignored) {
            nativePlayer = null;
        }

        if (nativePlayer == null) {
            return RCommandSources.of(
                platformId,
                source,
                Audience.empty(),
                Optional.empty(),
                permission -> permissionChecker.test(source, permission)
            );
        }

        RPlayer player = Rapunzel.players().require(nativePlayer);
        return RCommandSources.of(
            platformId,
            source,
            player.audience(),
            Optional.of(player),
            permission -> permissionChecker.test(source, permission)
        );
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T value) throws Exception;
    }
}
