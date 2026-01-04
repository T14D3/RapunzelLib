package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;

import java.lang.reflect.Method;
import java.util.ServiceLoader;

public final class GameEvents {
    private GameEvents() {
    }

    public static GameEventBus bus() {
        return Rapunzel.context().services().get(GameEventBus.class);
    }

    public static GameEventBus install(Object owner) {
        RapunzelContext ctx = Rapunzel.context();
        if (ctx.services().find(GameEventBridge.class).isPresent()) {
            return ctx.services().get(GameEventBus.class);
        }

        GameEventBus bus = ctx.services().find(GameEventBus.class).orElseGet(() -> {
            GameEventBus created = new GameEventBus(ctx.scheduler(), ctx.logger());
            boolean tracked = registerService(ctx, GameEventBus.class, created);
            if (!tracked) {
                registerCloseable(ctx, created);
            }
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
        boolean tracked = registerService(ctx, GameEventBridge.class, bridge);
        if (!tracked) {
            registerCloseable(ctx, bridge);
        }
        return bus;
    }

    private static <T> boolean registerService(RapunzelContext ctx, Class<T> type, T instance) {
        // Prefer DefaultRapunzelContext.register(...) so AutoCloseable services are tracked for shutdown.
        try {
            Method m = ctx.getClass().getMethod("register", Class.class, Object.class);
            m.invoke(ctx, type, instance);
            return true;
        } catch (Exception ignored) {
        }
        ctx.services().register(type, instance);
        return false;
    }

    private static void registerCloseable(RapunzelContext ctx, AutoCloseable closeable) {
        try {
            Method m = ctx.getClass().getMethod("registerCloseable", AutoCloseable.class);
            m.invoke(ctx, closeable);
        } catch (Exception ignored) {
        }
    }
}
