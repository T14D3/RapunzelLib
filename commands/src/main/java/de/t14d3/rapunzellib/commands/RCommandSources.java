package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import net.kyori.adventure.audience.Audience;

import java.util.Objects;
import java.util.Optional;

public final class RCommandSources {
    private RCommandSources() {
    }

    public static RCommandSource of(PlatformId platformId, Audience audience) {
        return of(platformId, audience, Optional.empty());
    }

    public static RCommandSource of(PlatformId platformId, Audience audience, RPlayer player) {
        return of(platformId, audience, Optional.of(player));
    }

    public static RCommandSource of(PlatformId platformId, Audience audience, Optional<RPlayer> player) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(player, "player");
        return new DefaultRCommandSource(platformId, audience, player);
    }

    private static final class DefaultRCommandSource extends RNativeHandle<Audience> implements RCommandSource {
        private final Optional<RPlayer> player;

        private DefaultRCommandSource(PlatformId platformId, Audience audience, Optional<RPlayer> player) {
            super(platformId, Objects.requireNonNull(audience, "audience"));
            this.player = Objects.requireNonNull(player, "player");
        }

        @Override
        public Audience audience() {
            return handle();
        }

        @Override
        public Optional<RPlayer> player() {
            return player;
        }
    }
}
