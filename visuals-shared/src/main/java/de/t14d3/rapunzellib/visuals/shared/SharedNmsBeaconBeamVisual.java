package de.t14d3.rapunzellib.visuals.shared;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.visuals.BeaconBeamConfig;
import de.t14d3.rapunzellib.visuals.BeaconBeamVisual;
import de.t14d3.rapunzellib.visuals.BeaconColorRenderer;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared NMS implementation of a beacon beam visual.
 * <p>
 * Sends block update packets to render a beacon block, an iron pyramid,
 * and an optional stained-glass column extending to the sky.
 */
public final class SharedNmsBeaconBeamVisual extends SharedNmsVisual<BeaconBeamConfig> implements BeaconBeamVisual {
    private final Map<UUID, List<BlockPos>> sentBlocksByViewer = new HashMap<>();
    private int currentPyramidLevels;
    private boolean currentExtendToSky;

    public SharedNmsBeaconBeamVisual(
        @NotNull VisualId id,
        @NotNull BeaconBeamConfig config,
        @NotNull VisualAudience audience,
        @NotNull VisualManager manager
    ) {
        super(id, config, audience, manager);
        this.currentPyramidLevels = config.pyramidLevels();
        this.currentExtendToSky = config.extendToSky();
    }

    @Override
    public void updatePyramid(int levels) {
        this.currentPyramidLevels = Math.max(0, Math.min(4, levels));
        reshowIfNeeded();
    }

    @Override
    public void updateGlassColumn(boolean extendToSky) {
        this.currentExtendToSky = extendToSky;
        reshowIfNeeded();
    }

    @Override
    protected @NotNull RLocation visibilityCenter() {
        return config.location();
    }

    @Override
    protected void spawnFor(@NotNull ServerPlayer player) {
        List<BlockPos> sentBlocks = new ArrayList<>();
        RBlockPos beacon = new RBlockPos(
            (int) Math.floor(config.location().x()),
            (int) Math.floor(config.location().y()),
            (int) Math.floor(config.location().z())
        );
        BlockPos beaconPos = toBlockPos(beacon);

        sendBlock(player, sentBlocks, beaconPos, Blocks.BEACON.defaultBlockState());

        if (currentPyramidLevels > 0) {
            BlockState ironState = Blocks.IRON_BLOCK.defaultBlockState();
            for (RBlockPos pos : BeaconGeometry.pyramid(beacon, currentPyramidLevels)) {
                sendBlock(player, sentBlocks, toBlockPos(pos), ironState);
            }
        }

        if (currentExtendToSky) {
            NamedTextColor baseColor = config.color() instanceof NamedTextColor named ? named : NamedTextColor.WHITE;
            BlockState glassState = getStainedGlassState(toDyeColor(baseColor));
            int height = Math.max(0, player.level().getHeight() - beaconPos.getY());
            List<RBlockPos> column = BeaconGeometry.glassColumn(beacon, height);
            BeaconColorRenderer renderer = config.colorRenderer();
            for (int i = 0; i < column.size(); i++) {
                RBlockPos pos = column.get(i);
                BlockState state = renderer != null
                    ? getStainedGlassState(toDyeColor(renderer.render(pos, i, column.size())))
                    : glassState;
                sendBlock(player, sentBlocks, toBlockPos(pos), state);
            }
        }

        sentBlocksByViewer.put(player.getUUID(), sentBlocks);
    }

    @Override
    protected void destroyFor(@NotNull ServerPlayer player) {
        List<BlockPos> sentBlocks = sentBlocksByViewer.remove(player.getUUID());
        if (sentBlocks == null) return;
        for (BlockPos pos : sentBlocks) {
            player.connection.send(new ClientboundBlockUpdatePacket(pos, player.level().getBlockState(pos)));
        }
    }

    /**
     * Refreshes the visual for all current viewers by re-sending all blocks.
     */
    public void refresh() {
        if (!shown) return;
        for (UUID uuid : currentViewerIds()) {
            RPlayer player = RPlayer.get(uuid).orElse(null);
            if (player == null) continue;
            ServerPlayer serverPlayer = tryUnwrap(player);
            if (serverPlayer == null) continue;
            destroyFor(serverPlayer);
            spawnFor(serverPlayer);
        }
    }

    private void reshowIfNeeded() {
        if (shown) {
            hide();
            show();
        }
    }

    private void sendBlock(
        @NotNull ServerPlayer player,
        @NotNull List<BlockPos> sentBlocks,
        @NotNull BlockPos pos,
        @NotNull BlockState state
    ) {
        player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
        sentBlocks.add(pos);
    }

    private static BlockPos toBlockPos(RBlockPos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    private static BlockState getStainedGlassState(DyeColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_STAINED_GLASS.defaultBlockState();
            case ORANGE -> Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
            case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            case YELLOW -> Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
            case LIME -> Blocks.LIME_STAINED_GLASS.defaultBlockState();
            case PINK -> Blocks.PINK_STAINED_GLASS.defaultBlockState();
            case GRAY -> Blocks.GRAY_STAINED_GLASS.defaultBlockState();
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState();
            case CYAN -> Blocks.CYAN_STAINED_GLASS.defaultBlockState();
            case PURPLE -> Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
            case BLUE -> Blocks.BLUE_STAINED_GLASS.defaultBlockState();
            case BROWN -> Blocks.BROWN_STAINED_GLASS.defaultBlockState();
            case GREEN -> Blocks.GREEN_STAINED_GLASS.defaultBlockState();
            case RED -> Blocks.RED_STAINED_GLASS.defaultBlockState();
            case BLACK -> Blocks.BLACK_STAINED_GLASS.defaultBlockState();
        };
    }

    private static DyeColor toDyeColor(@NotNull NamedTextColor color) {
        if (color == NamedTextColor.WHITE) return DyeColor.WHITE;
        if (color == NamedTextColor.GOLD) return DyeColor.ORANGE;
        if (color == NamedTextColor.LIGHT_PURPLE) return DyeColor.MAGENTA;
        if (color == NamedTextColor.AQUA) return DyeColor.LIGHT_BLUE;
        if (color == NamedTextColor.YELLOW) return DyeColor.YELLOW;
        if (color == NamedTextColor.GREEN) return DyeColor.LIME;
        if (color == NamedTextColor.RED) return DyeColor.RED;
        if (color == NamedTextColor.GRAY) return DyeColor.GRAY;
        if (color == NamedTextColor.DARK_GRAY) return DyeColor.LIGHT_GRAY;
        if (color == NamedTextColor.DARK_AQUA) return DyeColor.CYAN;
        if (color == NamedTextColor.DARK_PURPLE) return DyeColor.PURPLE;
        if (color == NamedTextColor.BLUE) return DyeColor.BLUE;
        if (color == NamedTextColor.DARK_GREEN) return DyeColor.GREEN;
        if (color == NamedTextColor.DARK_RED) return DyeColor.RED;
        if (color == NamedTextColor.BLACK) return DyeColor.BLACK;
        return DyeColor.WHITE;
    }
}
