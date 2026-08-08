package de.t14d3.rapunzellib.gui.shared.map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders live world terrain into a surface.
 * <p>
 * Rendering is split into two phases so the expensive per-column scan never
 * runs on the server thread:
 * </p>
 * <ol>
 *   <li>{@link #snapshot} runs on the main thread and copies everything the
 *   rasterizer needs out of the loaded chunks of the view - the world-surface
 *   heights and the block sections around them. It reads the world, but only
 *   once per chunk (a few memcpys), not once per pixel.</li>
 *   <li>{@link #rasterize} runs on any thread and is pure math over the
 *   snapshot: it never touches the world, the chunk, or their locks.</li>
 * </ol>
 * <p>
 * The snapshot holds only copies ({@link LevelChunkSection#copy} and cloned
 * height arrays), never chunk references, so a chunk being unloaded while the
 * rasterizer runs is harmless. Shading mimics vanilla maps - slope between
 * neighboring columns picks a brightness level, water is darkened by depth -
 * and pixels are written as packed map palette ids straight from
 * {@link MapColor#getPackedId}.
 * </p>
 */
public final class SharedMapTerrain {

    private static final int MAX_WATER_DEPTH = 32;

    /**
     * How many blocks below the lowest surface height in the view are copied
     * per chunk. Covers the water-depth walk and transparent block walks
     * (trees, snow, glass) without copying the whole chunk.
     */
    private static final int SECTION_MARGIN_BLOCKS = 48;

    private SharedMapTerrain() {
    }

    /**
     * The world data one chunk contributes to a view, copied on the main
     * thread and read-only afterwards.
     */
    private static final class ChunkSnapshot {
        private final int chunkX;
        private final int chunkZ;
        /** World-surface heights, one per column, index {@code localZ * 16 + localX}. */
        private final short[] heights;
        /** The section y of {@code sections[0]}. */
        private final int sectionBase;
        private final LevelChunkSection[] sections;

        private ChunkSnapshot(int chunkX, int chunkZ, short[] heights, int sectionBase, LevelChunkSection[] sections) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.heights = heights;
            this.sectionBase = sectionBase;
            this.sections = sections;
        }

        /**
         * The block at a local column and a world y, or {@code null} when the
         * y lies outside the copied sections (below the copied margin or above
         * the surface).
         */
        private BlockState blockState(int localX, int y, int localZ) {
            int index = (y >> 4) - sectionBase;
            if (index < 0 || index >= sections.length) {
                return null;
            }
            return sections[index].getBlockState(localX, y & 15, localZ);
        }
    }

    /**
     * An immutable copy of the world data the rasterizer needs for one view.
     * Safe to read from any thread.
     */
    public static final class TerrainSnapshot {
        private final int minY;
        private final Map<Long, ChunkSnapshot> chunks;

        private TerrainSnapshot(int minY, Map<Long, ChunkSnapshot> chunks) {
            this.minY = minY;
            this.chunks = chunks;
        }

        private ChunkSnapshot chunkAt(int worldX, int worldZ) {
            return chunks.get(key(worldX >> 4, worldZ >> 4));
        }

        private static long key(int chunkX, int chunkZ) {
            return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        }
    }

    /**
     * Copies the loaded chunks of the view into a snapshot.
     * <p>
     * Must run on the server's main thread: it reads the world and performs
     * chunk lookups.
     *
     * @param level          the world to read
     * @param centerBlockX   the block x the view is centered on
     * @param centerBlockZ   the block z the view is centered on
     * @param blocksPerPixel the zoom, in blocks per pixel
     * @param width          the view width in pixels
     * @param height         the view height in pixels
     * @return the snapshot, or an empty one when nothing is loaded
     */
    public static TerrainSnapshot snapshot(
        ServerLevel level,
        int centerBlockX,
        int centerBlockZ,
        int blocksPerPixel,
        int width,
        int height
    ) {
        int scale = Math.max(1, blocksPerPixel);
        int minX = centerBlockX - (width / 2) * scale;
        int minZ = centerBlockZ - (height / 2) * scale;
        int maxX = minX + width * scale - 1;
        int maxZ = minZ + height * scale - 1;

        Map<Long, ChunkSnapshot> chunks = new HashMap<>();
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                // load=false: never loads or generates; unloaded chunks stay blank.
                ChunkAccess chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                ChunkSnapshot snapshot = snapshotChunk(chunk);
                if (snapshot != null) {
                    chunks.put(TerrainSnapshot.key(chunkX, chunkZ), snapshot);
                }
            }
        }
        return new TerrainSnapshot(level.getMinY(), Map.copyOf(chunks));
    }

    private static ChunkSnapshot snapshotChunk(ChunkAccess chunk) {
        short[] heights = new short[256];
        int minSurface = Integer.MAX_VALUE;
        int maxSurface = Integer.MIN_VALUE;
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
                heights[localZ * 16 + localX] = (short) surface;
                minSurface = Math.min(minSurface, surface);
                maxSurface = Math.max(maxSurface, surface);
            }
        }

        int sectionBase = Math.max(chunk.getMinSectionY(), (minSurface - SECTION_MARGIN_BLOCKS) >> 4);
        int sectionTop = Math.min(chunk.getMaxSectionY(), maxSurface >> 4);
        int sectionCount = sectionTop - sectionBase + 1;
        LevelChunkSection[] sections = new LevelChunkSection[sectionCount];
        for (int sectionY = sectionBase; sectionY <= sectionTop; sectionY++) {
            // getSection takes an index into the chunk's section array, not a
            // world section y: a negative or world-relative value reads the
            // wrong (or an out-of-bounds) entry.
            sections[sectionY - sectionBase] = chunk.getSection(sectionY - chunk.getMinSectionY()).copy();
        }
        // #if VERSION >= 26.0.0
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        // #else
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        // #endif
        return new ChunkSnapshot(chunkX, chunkZ, heights, sectionBase, sections);
    }

    /**
     * Rasterizes a snapshot into packed map palette ids, one byte per pixel.
     * <p>
     * Pure math over the snapshot - no world, chunk, or lock access - so this
     * can run off the server thread.
     *
     * @param snapshot       the snapshot taken on the main thread
     * @param centerBlockX   the block x the view is centered on
     * @param centerBlockZ   the block z the view is centered on
     * @param blocksPerPixel the zoom, in blocks per pixel
     * @param width          the view width in pixels
     * @param height         the view height in pixels
     * @return the packed palette ids, row-major
     */
    public static byte[] rasterize(
        TerrainSnapshot snapshot,
        int centerBlockX,
        int centerBlockZ,
        int blocksPerPixel,
        int width,
        int height
    ) {
        int scale = Math.max(1, blocksPerPixel);
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        int minY = snapshot.minY;
        byte[] pixels = new byte[width * height];

        for (int px = 0; px < width; px++) {
            int worldX = centerBlockX + (px - halfWidth) * scale;
            double previousHeight = Double.NaN;

            for (int py = 0; py < height; py++) {
                int worldZ = centerBlockZ + (py - halfHeight) * scale;
                ChunkSnapshot chunk = snapshot.chunkAt(worldX, worldZ);
                if (chunk == null) {
                    pixels[py * width + px] = 0;
                    previousHeight = Double.NaN;
                    continue;
                }

                int localX = worldX & 15;
                int localZ = worldZ & 15;
                int surface = chunk.heights[localZ * 16 + localX];
                int visibleY = surface;
                BlockState state = chunk.blockState(localX, visibleY, localZ);
                while (visibleY > minY
                    && state != null
                    && state.getMapColor(null, null) == MapColor.NONE) {
                    visibleY--;
                    state = chunk.blockState(localX, visibleY, localZ);
                }
                if (state == null) {
                    // Below the copied sections; treat as air (transparent).
                    state = Blocks.AIR.defaultBlockState();
                }

                double currentHeight = visibleY + 1.0;
                if (Double.isNaN(previousHeight)) {
                    previousHeight = currentHeight;
                }

                MapColor.Brightness brightness = state.getFluidState().is(Fluids.WATER)
                    ? waterBrightness(chunk, localX, localZ, visibleY, minY, px, py)
                    : terrainBrightness(currentHeight, previousHeight, scale, px, py);

                pixels[py * width + px] = (byte) state.getMapColor(null, null).getPackedId(brightness);
                previousHeight = currentHeight;
            }
        }
        return pixels;
    }

    /**
     * Vanilla shades land by how much the ground rises or falls going north, plus a dither.
     */
    private static MapColor.Brightness terrainBrightness(double height, double previousHeight, int scale, int px, int py) {
        double slope = (height - previousHeight) * 4.0 / (scale + 4) + ((px + py & 1) - 0.5) * 0.4;
        if (slope > 0.6) {
            return MapColor.Brightness.HIGH;
        }
        if (slope < -0.6) {
            return MapColor.Brightness.LOW;
        }
        return MapColor.Brightness.NORMAL;
    }

    /** Deeper water is drawn darker, like a vanilla map. */
    private static MapColor.Brightness waterBrightness(
        ChunkSnapshot chunk,
        int localX,
        int localZ,
        int visibleY,
        int minY,
        int px,
        int py
    ) {
        int depth = 0;
        int y = visibleY;
        while (depth < MAX_WATER_DEPTH && y > minY) {
            BlockState current = chunk.blockState(localX, y, localZ);
            if (current == null || !current.getFluidState().is(Fluids.WATER)) {
                break;
            }
            y--;
            depth++;
        }
        double murk = depth * 0.1 + (px + py & 1) * 0.2;
        if (murk < 0.5) {
            return MapColor.Brightness.HIGH;
        }
        if (murk > 0.9) {
            return MapColor.Brightness.LOW;
        }
        return MapColor.Brightness.NORMAL;
    }
}
