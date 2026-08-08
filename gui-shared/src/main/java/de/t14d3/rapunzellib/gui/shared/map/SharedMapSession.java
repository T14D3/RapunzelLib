package de.t14d3.rapunzellib.gui.shared.map;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.gui.map.GuiMap;
import de.t14d3.rapunzellib.gui.map.GuiMapClick;
import de.t14d3.rapunzellib.gui.map.GuiMapLayer;
import de.t14d3.rapunzellib.gui.map.GuiMapPoint;
import de.t14d3.rapunzellib.gui.map.GuiMapViewport;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A live map session for one player.
 * <p>
 * The session owns the 128x128 surface, drives repaints on a 50ms tick, keeps
 * the viewport centered on the player, refreshes terrain only when they move
 * (and only as often as a throttle allows), and translates clicks into
 * pixel/block coordinates for the map's handler.
 * </p>
 * <p>
 * Repaint triggers are terrain movement, cursor movement, clicks, and a slow
 * steady cadence. Because the surface only dirties pixels that actually
 * change, an unchanged frame sends nothing - a moving selection highlight
 * sends only the highlight.
 * </p>
 */
public final class SharedMapSession {

    /** Map canvas size: a Minecraft map item is 128x128 pixels. */
    public static final int SIZE = 128;

    private static final Duration TICK = Duration.ofMillis(50);
    private static final Duration TERRAIN_THROTTLE = Duration.ofMillis(600);
    private static final Duration STEADY_CADENCE = Duration.ofSeconds(1);
    private static final long CLICK_DEDUP_MS = 50;
    private static final long FAKE_REASSERT_MS = 1000;

    /** Degrees of yaw that sweep the cursor across the whole canvas width. */
    private static final float YAW_RANGE = 180.0f;

    private final RPlayer player;
    private final ServerPlayer serverPlayer;
    private final GuiMap gui;
    private final Consumer<GuiMapClick> onClick;
    private final boolean terrainEnabled;
    private final int terrainScale;

    private final SharedMapSurface surface = new SharedMapSurface(SIZE, SIZE);
    private final SharedMapPalette palette = new SharedMapPalette();
    private final SharedMapPainter painter = new SharedMapPainter(surface, palette, new BuiltinMapFont());

    /**
     * The pristine terrain layer, kept separate from the frame surface.
     * <p>
     * Every repaint restores the frame from this base before drawing the GUI
     * layers on top. Without that, translucent overlays (zone tints) blend
     * against their own previous result and compound toward opaque on every
     * steady-cadence repaint.
     * </p>
     */
    private SharedMapSurface terrain;

    /**
     * The latest rasterized terrain frame, produced off-thread by
     * {@link SharedMapTerrain#rasterize} and blitted into {@link #terrain} by
     * the next tick.
     */
    private volatile byte[] pendingTerrain;

    /** True while a terrain render is in flight, so movements cannot stack renders. */
    private boolean terrainRenderInFlight;

    private final MapId mapId;
    private final ScheduledTask task;

    private GuiMapViewport viewport;
    private int cursorX = SIZE / 2;
    private int cursorY = SIZE / 2;
    private float lastYaw = Float.NaN;
    private long lastPaint;
    private long lastTerrainPaint;
    private int lastTerrainX = Integer.MIN_VALUE;
    private int lastTerrainZ = Integer.MIN_VALUE;
    private boolean firstPaint = true;
    private boolean clickPending;
    private boolean closed;
    private long lastClick;
    private long lastReassert;

    private SharedMapSession(@NotNull RPlayer player, @NotNull ServerPlayer serverPlayer, @NotNull GuiMap gui) {
        this.player = player;
        this.serverPlayer = serverPlayer;
        this.gui = gui;
        this.onClick = gui.onClick();
        this.terrainEnabled = gui.terrainBlocksPerPixel() > 0;
        this.terrainScale = Math.max(1, gui.terrainBlocksPerPixel());

        this.mapId = SharedMapTransport.allocateId();
        SharedMapTransport.showFakeMap(serverPlayer, mapId);
        this.viewport = GuiMapViewport.of(playerBlockX(), playerBlockZ(), terrainScale, SIZE, SIZE);

        this.task = Rapunzel.scheduler().runRepeating(Duration.ZERO, TICK, this::tick);
        SharedMapTransport.sendFull(serverPlayer, mapId, surface);
    }

    /**
     * Opens a map session for a player, replacing any existing session.
     *
     * @param gui    the map to show
     * @param player the player
     */
    public static void open(@NotNull GuiMap gui, @NotNull RPlayer player) {
        Objects.requireNonNull(gui, "gui");
        Objects.requireNonNull(player, "player");
        SharedMapSessions.close(player.uuid());
        ServerPlayer serverPlayer = ((RNativeHandle<ServerPlayer>) player).handle();
        SharedMapSessions.register(player.uuid(), new SharedMapSession(player, serverPlayer, gui));
    }

    /** The player this session serves. */
    public @NotNull RPlayer player() {
        return player;
    }

    /**
     * Handles a click on the map. The click position is wherever the player
     * is currently looking.
     *
     * @param action which button was pressed
     */
    public void activate(@NotNull GuiMapClick.Action action) {
        if (closed || onClick == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long previous = lastClick;
        lastClick = now;
        if (now - previous < CLICK_DEDUP_MS) {
            return;
        }
        GuiMapPoint pixel = new GuiMapPoint(cursorX, cursorY);
        GuiMapClick click = new GuiMapClick(pixel, viewport.pixelToWorld(pixel), action);
        clickPending = true;
        try {
            onClick.accept(click);
        } catch (RuntimeException error) {
            Rapunzel.logger().error("Map click handler failed", error);
        }
    }

    /** Closes the session, reveals the player's real hotbar and stops the tick. */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        task.cancel();
        SharedMapTransport.hideFakeMap(serverPlayer);
    }

    // ---- tick ----

    private void tick() {
        if (closed) {
            return;
        }
        updateCursor();
        updateViewport();

        boolean terrainDue = terrainMoved() && due(lastTerrainPaint, TERRAIN_THROTTLE);
        boolean due = terrainDue
            || clickPending
            || cursorMoved
            || firstPaint
            || due(lastPaint, STEADY_CADENCE);
        if (!due) {
            return;
        }

        if (terrainEnabled) {
            if (terrainDue && !terrainRenderInFlight) {
                int centerX = viewport.centerBlockX();
                int centerZ = viewport.centerBlockZ();
                // Snapshot on the main thread (reads the world), rasterize off it.
                SharedMapTerrain.TerrainSnapshot snapshot = SharedMapTerrain.snapshot(
                    serverPlayer.level(), centerX, centerZ, terrainScale, SIZE, SIZE
                );
                terrainRenderInFlight = true;
                Rapunzel.scheduler().runAsync(() -> {
                    byte[] pixels = SharedMapTerrain.rasterize(snapshot, centerX, centerZ, terrainScale, SIZE, SIZE);
                    pendingTerrain = pixels;
                });
                lastTerrainX = centerX;
                lastTerrainZ = centerZ;
                lastTerrainPaint = System.currentTimeMillis();
            }
            byte[] freshTerrain = pendingTerrain;
            if (freshTerrain != null) {
                pendingTerrain = null;
                terrainRenderInFlight = false;
                if (terrain == null) {
                    terrain = new SharedMapSurface(SIZE, SIZE);
                }
                for (int y = 0, i = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++, i++) {
                        terrain.set(x, y, freshTerrain[i]);
                    }
                }
            }
            // Restore the pristine terrain base before drawing layers, so
            // translucent overlays blend against it rather than against their
            // own previous result. The snapshot was taken at lastTerrainX/Z;
            // if the viewport has moved since, the base is shifted by that
            // delta so terrain stays aligned with the overlays even while the
            // async rasterize is in flight.
            if (terrain != null) {
                int offsetX = Math.floorDiv(viewport.centerBlockX() - lastTerrainX, terrainScale);
                int offsetZ = Math.floorDiv(viewport.centerBlockZ() - lastTerrainZ, terrainScale);
                if (offsetX <= -SIZE || offsetX >= SIZE || offsetZ <= -SIZE || offsetZ >= SIZE) {
                    painter.clear(de.t14d3.rapunzellib.gui.map.GuiMapColor.BLACK);
                } else {
                    for (int y = 0; y < SIZE; y++) {
                        for (int x = 0; x < SIZE; x++) {
                            int srcX = x + offsetX;
                            int srcY = y + offsetZ;
                            surface.set(x, y, srcX >= 0 && srcX < SIZE && srcY >= 0 && srcY < SIZE
                                ? terrain.get(srcX, srcY)
                                : 0);
                        }
                    }
                }
            }
        } else {
            // Restore the background every frame, mirroring the terrain path above.
            painter.clear(de.t14d3.rapunzellib.gui.map.GuiMapColor.BLACK);
        }

        for (GuiMapLayer layer : gui.layers()) {
            try {
                layer.draw(painter, viewport);
            } catch (RuntimeException error) {
                Rapunzel.logger().error("Map layer draw failed", error);
            }
        }

        firstPaint = false;
        clickPending = false;
        cursorMoved = false;
        lastPaint = System.currentTimeMillis();
        SharedMapTransport.sendDirty(serverPlayer, mapId, surface);
        surface.clearDirty();

        // Inventory resyncs (a denied interaction, a respawn, ...) resend the
        // real slots and would reveal the truth; re-assert the fake hotbar.
        if (System.currentTimeMillis() - lastReassert >= FAKE_REASSERT_MS) {
            lastReassert = System.currentTimeMillis();
            SharedMapTransport.reassertFakeMap(serverPlayer, mapId);
        }
    }

    private boolean cursorMoved;

    private boolean terrainMoved() {
        return lastTerrainX == Integer.MIN_VALUE
            || viewport.centerBlockX() != lastTerrainX
            || viewport.centerBlockZ() != lastTerrainZ;
    }

    private static boolean due(long last, Duration throttle) {
        return System.currentTimeMillis() - last >= throttle.toMillis();
    }

    // ---- cursor & viewport ----

    private void updateViewport() {
        viewport = viewport.withCenter(playerBlockX(), playerBlockZ());
    }

    private void updateCursor() {
        // Yaw accumulates (the player can keep turning); pitch maps
        // absolutely, so looking straight down is the bottom of the map.
        float yaw = serverPlayer.getYRot();
        float pitch = serverPlayer.getXRot();
        if (!Float.isNaN(lastYaw)) {
            float delta = wrap180(yaw - lastYaw);
            if (delta != 0) {
                int x = Math.round(cursorX + delta * (SIZE / YAW_RANGE));
                cursorX = Math.max(0, Math.min(SIZE - 1, x));
                cursorMoved = true;
            }
        }
        lastYaw = yaw;

        int y = Math.round((pitch + 90.0f) / 180.0f * SIZE);
        y = Math.max(0, Math.min(SIZE - 1, y));
        if (y != cursorY) {
            cursorY = y;
            cursorMoved = true;
        }
    }

    private static float wrap180(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped > 180.0f) {
            wrapped -= 360.0f;
        } else if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private int playerBlockX() {
        return serverPlayer.getBlockX();
    }

    private int playerBlockZ() {
        return serverPlayer.getBlockZ();
    }
}
