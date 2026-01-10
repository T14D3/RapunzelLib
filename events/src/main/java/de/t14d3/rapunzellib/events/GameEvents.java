package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;

public final class GameEvents {
    private GameEvents() {
    }

    public static @NotNull GameEventBus bus() {
        return Rapunzel.context().services().get(GameEventBus.class);
    }

    public static @NotNull GameEventBus install(@Nullable Object owner) {
        RapunzelContext ctx = Rapunzel.context();
        if (ctx.services().find(GameEventBridge.class).isPresent()) {
            return ctx.services().get(GameEventBus.class);
        }

        GameEventBus bus = ctx.services().find(GameEventBus.class).orElseGet(() -> {
            GameEventBus created = new GameEventBus(ctx.scheduler(), ctx.logger());
            ctx.register(GameEventBus.class, created);
            return created;
        });

        GameEventBridgeInstaller installer = ServiceLoader.load(GameEventBridgeInstaller.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .filter(i -> i.platformId() == ctx.platformId())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No GameEventBridgeInstaller found for platform " + ctx.platformId() +
                    " (did you add rapunzellib-events-" + ctx.platformId().name().toLowerCase() + "?)"
            ));

        GameEventBridge bridge = installer.install(ctx, bus, owner);
        ctx.register(GameEventBridge.class, bridge);
        return bus;
    }
}
