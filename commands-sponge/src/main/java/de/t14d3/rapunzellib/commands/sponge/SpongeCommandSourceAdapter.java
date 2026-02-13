package de.t14d3.rapunzellib.commands.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.AudienceCommandSourceAdapterCore;
import de.t14d3.rapunzellib.commands.RCommandSource;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;

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
        return AudienceCommandSourceAdapterCore.wrap(
            PlatformId.SPONGE,
            source,
            s -> audience,
            (s, permission) -> s instanceof Subject subject && subject.hasPermission(permission),
            s -> (s instanceof ServerPlayer spongePlayer)
                ? Rapunzel.players().wrap(spongePlayer)
                : java.util.Optional.empty()
        );
    }

    public static RCommandSource wrap(CommandCause cause) {
        Objects.requireNonNull(cause, "cause");

        return AudienceCommandSourceAdapterCore.wrap(
            PlatformId.SPONGE,
            cause,
            CommandCause::audience,
            CommandCause::hasPermission,
            c -> c.first(ServerPlayer.class)
                .flatMap(player -> Rapunzel.players().wrap(player))
        );
    }
}
