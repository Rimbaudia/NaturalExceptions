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
}
