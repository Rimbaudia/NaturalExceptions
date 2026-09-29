package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** Regression: the smelter's contents disappeared whenever its front block was out of view. */
final class SmelterRenderBoundsTest {

    @Test
    void renderBoxCoversTheWholeCavityBehindTheController() {
        // Controller at (10, 64, 10) facing north; a 1x3x1 shaft behind it at z = 11, y = 64..66.
        int[] box = TileEntityStarterSmelter.renderBounds(
            10,
            64,
            10,
            Arrays.asList(new int[] { 10, 64, 11 }, new int[] { 10, 65, 11 }, new int[] { 10, 66, 11 }));
        assertArrayEquals(new int[] { 9, 63, 9, 12, 68, 13 }, box);
    }

    @Test
    void contentsAreDrawnInsideTheCavityRelativeToTheController() {
        // Regression: the contents were drawn around the camera. The box must be the cavity, relative to the
        // controller, for every facing: here a 3x2x3 cavity west of a controller facing east.
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        for (int y = 64; y < 66; y++)
            for (int x = 6; x < 9; x++) for (int z = 9; z < 12; z++) cells.add(new int[] { x, y, z });
        double[] box = TileEntityStarterSmelter.cavityBoxRelative(10, 64, 10, cells);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { -4, 0, -1, -1, 2, 2 }, box, 1.0e-9);
    }
}
