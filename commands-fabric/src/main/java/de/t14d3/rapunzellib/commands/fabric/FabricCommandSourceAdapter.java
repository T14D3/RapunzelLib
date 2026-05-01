package de.t14d3.rapunzellib.commands.fabric;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.AudienceCommandSourceAdapterCore;
import de.t14d3.rapunzellib.commands.RCommandSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
// #if VERSION >= 1.21.11
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
// #endif
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

final class FabricCommandSourceAdapter {
    private static final int OPERATOR_PERMISSION_LEVEL = 4;

    private FabricCommandSourceAdapter() {
    }

    static @NotNull RCommandSource wrap(@NotNull CommandSourceStack source) {
        Objects.requireNonNull(source, "source");

        return AudienceCommandSourceAdapterCore.wrap(
            PlatformId.FABRIC,
            source,
            FabricCommandSourceAdapter::audience,
            FabricCommandSourceAdapter::hasPermission,
            FabricCommandSourceAdapter::player
        );
    }

    private static @NotNull Audience audience(@NotNull CommandSourceStack source) {
        ServerPlayer player = nativePlayer(source);
        return player != null ? (Audience) player : new FabricCommandSourceAudience(source);
    }

    private static boolean hasPermission(@NotNull CommandSourceStack source, @NotNull String permission) {
        if (permission.isBlank()) {
            return true;
        }

        return player(source)
            .map(player -> player.hasPermission(permission))
            .orElseGet(() -> hasOperatorPermission(source));
    }

    private static @NotNull Optional<de.t14d3.rapunzellib.objects.RPlayer> player(@NotNull CommandSourceStack source) {
        ServerPlayer player = nativePlayer(source);
        if (player == null) {
            return Optional.empty();
        }
        return Rapunzel.players().wrap(player);
    }

    private static boolean hasOperatorPermission(@NotNull CommandSourceStack source) {
        // #if VERSION >= 1.21.11
        return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS));
        // #else
        return source.hasPermission(OPERATOR_PERMISSION_LEVEL);
        // #endif
    }

    private static @Nullable ServerPlayer nativePlayer(@NotNull CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    private static net.minecraft.network.chat.Component toNative(@NotNull Component component) {
        String json = GsonComponentSerializer.gson().serialize(component);
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static final class FabricCommandSourceAudience implements Audience {
        private final CommandSourceStack source;

        private FabricCommandSourceAudience(@NotNull CommandSourceStack source) {
            this.source = Objects.requireNonNull(source, "source");
        }

        @Override
        public void sendMessage(@NotNull Identity sender, @NotNull Component message, @NotNull MessageType type) {
            source.sendSystemMessage(toNative(message));
        }
    }
}
