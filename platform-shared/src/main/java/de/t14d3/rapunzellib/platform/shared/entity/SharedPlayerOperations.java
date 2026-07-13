package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.objects.RGameMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SharedPlayerOperations {

    private SharedPlayerOperations() {
    }

    public static void setGameMode(@NotNull ServerPlayer player, @NotNull RGameMode gameMode) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(gameMode, "gameMode");
        player.setGameMode(toNative(gameMode));
    }

    public static @NotNull RGameMode gameMode(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return fromNative(player.gameMode.getGameModeForPlayer());
    }

    public static void setOp(@NotNull ServerPlayer player, boolean op) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (op) {
            server.getPlayerList().op(player.nameAndId());
        } else {
            server.getPlayerList().deop(player.nameAndId());
        }
    }

    public static boolean isOp(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return ((ServerLevel) player.level()).getServer().getPlayerList().isOp(player.nameAndId());
    }

    private static @NotNull GameType toNative(@NotNull RGameMode mode) {
        return switch (mode) {
            case SURVIVAL -> GameType.SURVIVAL;
            case CREATIVE -> GameType.CREATIVE;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SPECTATOR;
        };
    }

    private static @NotNull RGameMode fromNative(@NotNull GameType type) {
        return switch (type) {
            case SURVIVAL -> RGameMode.SURVIVAL;
            case CREATIVE -> RGameMode.CREATIVE;
            case ADVENTURE -> RGameMode.ADVENTURE;
            case SPECTATOR -> RGameMode.SPECTATOR;
            default -> RGameMode.SURVIVAL;
        };
    }
}
