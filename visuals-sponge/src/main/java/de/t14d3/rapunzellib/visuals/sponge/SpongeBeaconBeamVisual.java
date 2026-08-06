package de.t14d3.rapunzellib.visuals.sponge;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.visuals.BeaconBeamConfig;
import de.t14d3.rapunzellib.visuals.BeaconBeamVisual;
import de.t14d3.rapunzellib.visuals.BeaconColorRenderer;
import de.t14d3.rapunzellib.visuals.DisplayTransform;
import de.t14d3.rapunzellib.visuals.VisualAudience;
import de.t14d3.rapunzellib.visuals.VisualId;
import de.t14d3.rapunzellib.visuals.VisualManager;
import de.t14d3.rapunzellib.visuals.shared.BeaconGeometry;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.entity.display.BlockDisplay;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Sponge API implementation of a beacon beam visual.
 * <p>
 * Sponge has no native beacon-beam rendering API, so the beam is approximated
 * with real {@code minecraft:block_display} entities: a beacon block at the
 * configured location, an iron-block pyramid below it, and (optionally) a
 * stained-glass column extending upward toward the sky.
 */
public final class SpongeBeaconBeamVisual extends SpongeVisual<BeaconBeamConfig> implements BeaconBeamVisual {

    /** The identity transform used for the individual beam block displays. */
    private static final DisplayTransform IDENTITY = DisplayTransform.identity();

    private volatile BlockDisplay beaconEntity;
    private final List<BlockDisplay> pyramidEntities = new ArrayList<>();
    private final List<BlockDisplay> columnEntities = new ArrayList<>();

    private volatile int currentPyramidLevels;
    private volatile boolean currentExtendToSky;

    public SpongeBeaconBeamVisual(
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
        if (shown) {
            despawnEntities(pyramidEntities);
            spawnPyramid();
        }
    }

    @Override
    public void updateGlassColumn(boolean extendToSky) {
        this.currentExtendToSky = extendToSky;
        if (shown) {
            despawnEntities(columnEntities);
            spawnGlassColumn();
        }
    }

    @Override
    protected @NotNull RLocation visibilityCenter() {
        return config.location();
    }

    @Override
    protected void spawnEntities() {
        ServerWorld world = serverWorld(config.location().world());
        if (world == null) return;
        if (beaconEntity != null && !beaconEntity.isRemoved()) return;

        RBlockPos beaconPos = beaconPos();
        Vector3d beaconCenter = blockCenter(beaconPos);

        beaconEntity = spawnBlockDisplay(world, beaconCenter, RBlockType.require("minecraft:beacon"), IDENTITY, false);
        spawnPyramid();
        spawnGlassColumn();
    }

    @Override
    protected void despawnEntities() {
        despawn(beaconEntity);
        beaconEntity = null;
        despawnEntities(pyramidEntities);
        despawnEntities(columnEntities);
        despawnTracked();
    }

    @Override
    protected void ensureEntitiesSpawned() {
        // Re-spawn any beam entities that were removed (e.g. by a chunk unload).
        if (beaconEntity == null || beaconEntity.isRemoved()) {
            spawnEntities();
        }
    }

    /**
     * Spawns the iron-block pyramid displays for the current level count.
     */
    private void spawnPyramid() {
        if (currentPyramidLevels <= 0) return;
        ServerWorld world = serverWorld(config.location().world());
        if (world == null) return;

        RBlockPos beaconPos = beaconPos();
        RBlockType ironBlock = RBlockType.require("minecraft:iron_block");
        for (RBlockPos pos : BeaconGeometry.pyramid(beaconPos, currentPyramidLevels)) {
            pyramidEntities.add(spawnBlockDisplay(world, blockCenter(pos), ironBlock, IDENTITY, false));
        }
    }

    /**
     * Spawns the stained-glass column displays when the beam extends to the sky.
     */
    private void spawnGlassColumn() {
        if (!currentExtendToSky) return;
        ServerWorld world = serverWorld(config.location().world());
        if (world == null) return;

        RBlockPos beaconPos = beaconPos();
        int height = Math.max(0, world.maximumHeight() - beaconPos.y());
        if (height <= 0) return;

        List<RBlockPos> column = BeaconGeometry.glassColumn(beaconPos, height);
        BeaconColorRenderer renderer = config.colorRenderer();
        for (int i = 0; i < column.size(); i++) {
            RBlockPos pos = column.get(i);
            NamedTextColor color = renderer != null
                ? renderer.render(pos, i, column.size())
                : baseColor();
            RBlockType glass = stainedGlass(color);
            columnEntities.add(spawnBlockDisplay(world, blockCenter(pos), glass, IDENTITY, false));
        }
    }

    private static void despawnEntities(@NotNull List<BlockDisplay> entities) {
        for (BlockDisplay entity : entities) {
            despawn(entity);
        }
        entities.clear();
    }

    /**
     * Returns the block position of the beacon itself (floored location).
     *
     * @return the beacon block position
     */
    private RBlockPos beaconPos() {
        return new RBlockPos(
            (int) Math.floor(config.location().x()),
            (int) Math.floor(config.location().y()),
            (int) Math.floor(config.location().z())
        );
    }

    private static @NotNull Vector3d blockCenter(@NotNull RBlockPos pos) {
        return new Vector3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
    }

    /**
     * Resolves the beam's base color, defaulting to white when the configured
     * color is not a named text color.
     *
     * @return the base named color
     */
    private @NotNull NamedTextColor baseColor() {
        return config.color() instanceof NamedTextColor named ? named : NamedTextColor.WHITE;
    }

    /**
     * Resolves the stained-glass block type matching a named text color.
     *
     * @param color the color
     * @return the stained-glass block type
     */
    private static @NotNull RBlockType stainedGlass(@NotNull NamedTextColor color) {
        String key;
        if (color == NamedTextColor.WHITE) {
            key = "white";
        } else if (color == NamedTextColor.GOLD) {
            key = "orange";
        } else if (color == NamedTextColor.LIGHT_PURPLE) {
            key = "magenta";
        } else if (color == NamedTextColor.AQUA) {
            key = "light_blue";
        } else if (color == NamedTextColor.YELLOW) {
            key = "yellow";
        } else if (color == NamedTextColor.GREEN) {
            key = "lime";
        } else if (color == NamedTextColor.GRAY) {
            key = "gray";
        } else if (color == NamedTextColor.DARK_GRAY) {
            key = "light_gray";
        } else if (color == NamedTextColor.DARK_AQUA) {
            key = "cyan";
        } else if (color == NamedTextColor.DARK_PURPLE) {
            key = "purple";
        } else if (color == NamedTextColor.BLUE || color == NamedTextColor.DARK_BLUE) {
            key = "blue";
        } else if (color == NamedTextColor.DARK_GREEN) {
            key = "green";
        } else if (color == NamedTextColor.RED || color == NamedTextColor.DARK_RED) {
            key = "red";
        } else if (color == NamedTextColor.BLACK) {
            key = "black";
        } else {
            key = "white";
        }
        return RBlockType.require("minecraft:" + key + "_stained_glass");
    }
}
