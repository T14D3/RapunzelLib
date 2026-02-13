package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class AudienceCommandSourceAdapterCore {
    private AudienceCommandSourceAdapterCore() {
    }

    public static <T> @NotNull RCommandSource wrap(
        @NotNull PlatformId platformId,
        @NotNull T source,
        @NotNull Function<? super T, ? extends Audience> audienceExtractor,
        @NotNull BiPredicate<? super T, ? super String> permissionChecker,
        @NotNull Function<? super T, Optional<RPlayer>> playerExtractor
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(audienceExtractor, "audienceExtractor");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        Objects.requireNonNull(playerExtractor, "playerExtractor");

        Audience audience = Objects.requireNonNull(audienceExtractor.apply(source), "audience");
        Optional<RPlayer> player = Objects.requireNonNull(playerExtractor.apply(source), "player");
        return RCommandSources.of(platformId, source, audience, player, permission -> permissionChecker.test(source, permission));
    }
}
