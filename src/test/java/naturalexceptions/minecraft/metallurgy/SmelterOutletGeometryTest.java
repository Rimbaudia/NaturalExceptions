package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SmelterOutletGeometryTest {

    @Test
    void channelHasOpenAirAboveFloorAndSelectionDoesNotFillTheBlock() {
        for (int facing = 2; facing <= 5; facing++) {
            double[] bounds = SmelterOutletGeometry.bounds(facing);
            assertTrue(bounds[4] - bounds[1] < 0.5);
            for (double[] box : SmelterOutletGeometry.parts(facing)) {
                boolean coversChannel = 0.5 > box[0] && 0.5 < box[3]
                    && 0.5 > box[1]
                    && 0.5 < box[4]
                    && 0.5 > box[2]
                    && 0.5 < box[5];
                assertTrue(!coversChannel, "The center above the channel must remain open");
            }
        }
    }

    @Test
    void mountingPlateTouchesSmelterAndSpoutEndsAtBlockCenterInEveryOrientation() {
        for (int facing = 2; facing <= 5; facing++) {
            double[] plate = SmelterOutletGeometry.parts(facing)[0];
            assertEquals(
                facing == 2 || facing == 4 ? 1.0 : 0.0,
                facing == 2 ? plate[5] : facing == 3 ? plate[2] : facing == 4 ? plate[3] : plate[0]);
            assertEquals(0.5, SmelterOutletGeometry.STREAM_X);
            assertEquals(0.5, SmelterOutletGeometry.STREAM_Z);
            for (double[] part : SmelterOutletGeometry.parts(facing)) {
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(part[axis] >= 0 && part[axis + 3] <= 1);
                    assertTrue(part[axis] < part[axis + 3]);
                }
            }
        }
    }
}
