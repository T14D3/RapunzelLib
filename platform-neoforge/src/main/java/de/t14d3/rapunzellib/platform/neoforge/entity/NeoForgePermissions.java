package de.t14d3.rapunzellib.platform.neoforge.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.exceptions.UnregisteredPermissionException;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

final class NeoForgePermissions {
    private static final ConcurrentHashMap<String, PermissionNode<Boolean>> NODES = new ConcurrentHashMap<>();

    private NeoForgePermissions() {
    }

    static boolean hasPermission(ServerPlayer player, String permission) {
        if (permission == null || permission.isBlank()) return true;        

        Objects.requireNonNull(player, "player");

        Identifier key = Identifier.tryParse(permission);
        if (key == null) {
            // NeoForge permissions are node-based; fall back for non-resource identifiers (e.g. "myplugin.foo").
            return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS));
        }

        PermissionNode<Boolean> node = NODES.computeIfAbsent(key.toString(), ignored -> new PermissionNode<>(
            key,
            PermissionTypes.BOOLEAN,
            (p, uuid, dynamics) -> p != null && p.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS))
        ));

        try {
            return PermissionAPI.getPermission(player, node);
        } catch (UnregisteredPermissionException ignored) {
            // If no permission handler has registered this node, fall back to a reasonable default.
            return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS));
        }
    }
}
