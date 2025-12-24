package de.t14d3.rapunzellib.platform.fabric.entity;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class FabricPermissions {
    private static final Method PERMISSIONS_CHECK;

    static {
        Method method = null;
        try {
            Class<?> perms = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            method = perms.getMethod("check", UUID.class, String.class);
        } catch (Exception ignored) {
        }
        PERMISSIONS_CHECK = method;
    }

    private FabricPermissions() {
    }

    static boolean hasPermission(ServerPlayer player, String permission) {
        if (PERMISSIONS_CHECK != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean> result = (CompletableFuture<Boolean>) PERMISSIONS_CHECK.invoke(null, player.getUUID(), permission);
                return result.join();
            } catch (Exception ignored) {
                // fall through
            }
        }

        // Reasonable default: treat level 4 as "op".
        return player.hasPermissions(4);
    }
}

