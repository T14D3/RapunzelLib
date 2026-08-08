package de.t14d3.rapunzellib.gui.map;

import de.t14d3.rapunzellib.objects.RBlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiMapViewportTest {

    private static final int SIZE = 128;

    @Test
    void pixelToWorldCentersOnBlock() {
        GuiMapViewport viewport = GuiMapViewport.of(100, 200, 1, SIZE, SIZE);
        // The center pixel covers the center block.
        assertEquals(new RBlockPos(100, 0, 200), viewport.pixelToWorld(new GuiMapPoint(64, 64)));
        // Top-left pixel is half a canvas north-west.
        assertEquals(new RBlockPos(100 - 64, 0, 200 - 64), viewport.pixelToWorld(new GuiMapPoint(0, 0)));
        // Bottom-right pixel covers the far south-east block.
        assertEquals(new RBlockPos(100 + 63, 0, 200 + 63), viewport.pixelToWorld(new GuiMapPoint(127, 127)));
    }

    @Test
    void worldToPixelIsTheInverseOnCellOrigins() {
        GuiMapViewport viewport = GuiMapViewport.of(0, 0, 1, SIZE, SIZE);
        for (int blockX = -200; blockX <= 200; blockX += 13) {
            for (int blockZ = -200; blockZ <= 200; blockZ += 17) {
                GuiMapPoint pixel = viewport.worldToPixel(blockX, blockZ);
                assertEquals(new RBlockPos(blockX, 0, blockZ), viewport.pixelToWorld(pixel));
            }
        }
    }

    @Test
    void worldToPixelProjectsCellsOntoSinglePixels() {
        GuiMapViewport viewport = GuiMapViewport.of(0, 0, 2, SIZE, SIZE);
        // All four blocks of a 2x2 column land on the same pixel.
        GuiMapPoint a = viewport.worldToPixel(0, 0);
        GuiMapPoint b = viewport.worldToPixel(1, 0);
        GuiMapPoint c = viewport.worldToPixel(0, 1);
        GuiMapPoint d = viewport.worldToPixel(1, 1);
        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(a, d);
        // ...and that pixel maps back to the cell origin.
        assertEquals(new RBlockPos(0, 0, 0), viewport.pixelToWorld(a));
    }

    @Test
    void negativeCoordinatesFloorCorrectly() {
        GuiMapViewport viewport = GuiMapViewport.of(-50, -50, 1, SIZE, SIZE);
        // -50 - 64 = -114, one pixel per block.
        assertEquals(new RBlockPos(-114, 0, -114), viewport.pixelToWorld(new GuiMapPoint(0, 0)));
        // Round-trip through a negative block.
        assertEquals(new GuiMapPoint(64, 64), viewport.worldToPixel(-50, -50));
        assertEquals(new RBlockPos(-50, 0, -50), viewport.pixelToWorld(viewport.worldToPixel(-50, -50)));
    }

    @Test
    void rectIntersectsAndContains() {
        GuiMapRect rect = new GuiMapRect(10, 10, 20, 20);
        assertTrue(rect.contains(10, 10));
        assertTrue(rect.contains(29, 29));
        assertFalse(rect.contains(30, 30));
        assertFalse(rect.contains(9, 15));

        GuiMapRect overlap = rect.intersect(new GuiMapRect(20, 20, 50, 50));
        assertEquals(new GuiMapRect(20, 20, 10, 10), overlap);

        GuiMapRect disjoint = rect.intersect(new GuiMapRect(100, 100, 5, 5));
        assertEquals(new GuiMapRect(100, 100, 0, 0), disjoint);
    }

    @Test
    void colorMixingAndScaling() {
        GuiMapColor white = GuiMapColor.WHITE;
        GuiMapColor black = GuiMapColor.BLACK;
        assertEquals(GuiMapColor.of(0x808080), white.mix(black, 0.5));
        assertEquals(GuiMapColor.of(0xFFFFFF), white.scaled(1.0));
        assertEquals(GuiMapColor.of(0x7F7F7F), white.scaled(0.499));
        assertEquals(0, GuiMapColor.TRANSPARENT.a());
        assertEquals(128, GuiMapColor.of(0xFF0000).withAlpha(128).a());
    }
}
