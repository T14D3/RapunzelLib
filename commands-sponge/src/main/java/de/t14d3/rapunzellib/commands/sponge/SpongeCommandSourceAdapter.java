package de.t14d3.rapunzellib.commands.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.RCommandSources;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Optional;
import java.util.Objects;

public final class SpongeCommandSourceAdapter {
    private SpongeCommandSourceAdapter() {
    }

    public static RCommandSource wrap(Object source) {
        Objects.requireNonNull(source, "source");

        if (source instanceof CommandCause cause) {
            return wrap(cause);
        }

        if (!(source instanceof Audience audience)) {
            throw new IllegalArgumentException(
                "Command source does not implement Adventure Audience: " + source.getClass().getName()
            );
        }

        Optional<RPlayer> player = (source instanceof ServerPlayer spongePlayer)
            ? Rapunzel.players().wrap(spongePlayer)
            : Optional.empty();

        return RCommandSources.of(PlatformId.SPONGE, audience, player);
    }

    public static RCommandSource wrap(CommandCause cause) {
        Objects.requireNonNull(cause, "cause");

        Audience audience = cause.audience();
        Optional<RPlayer> player = cause.first(ServerPlayer.class)
            .flatMap(p -> Rapunzel.players().wrap(p));

        return RCommandSources.of(PlatformId.SPONGE, audience, player);
    }
}
