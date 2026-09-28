package naturalexceptions.minecraft.metallurgy;

/** Shared model for rendering, selection and collision. North faces away from a south-side vessel. */
public final class SmelterOutletGeometry {

    public static final double STREAM_X = 0.5;
    public static final double STREAM_Y = 0.25;
    public static final double STREAM_Z = 0.5;

    private SmelterOutletGeometry() {}

    public static double[][] parts(int facing) {
        double[][] boxes = { { 0.25, 0.25, 0.875, 0.75, 0.6875, 1.0 }, // wall flange (clay)
            { 0.3125, 0.3125, 0.4375, 0.6875, 0.375, 0.875 }, // channel floor (sooted)
            { 0.25, 0.3125, 0.4375, 0.3125, 0.5, 0.875 }, // left rim (clay)
            { 0.6875, 0.3125, 0.4375, 0.75, 0.5, 0.875 }, // right rim (clay)
            { 0.375, 0.25, 0.4375, 0.625, 0.3125, 0.5625 } // downturned pouring lip (sooted)
        };
        for (double[] b : boxes) {
            double minX = b[0], minZ = b[2], maxX = b[3], maxZ = b[5];
            if (facing == 3) {
                b[2] = 1 - maxZ;
                b[5] = 1 - minZ;
            } else if (facing == 4) {
                b[0] = minZ;
                b[3] = maxZ;
                b[2] = minX;
                b[5] = maxX;
            } else if (facing == 5) {
                b[0] = 1 - maxZ;
                b[3] = 1 - minZ;
                b[2] = minX;
                b[5] = maxX;
            }
        }
        return boxes;
    }

    public static double[] bounds(int facing) {
        double[] result = { 1, 1, 1, 0, 0, 0 };
        for (double[] part : parts(facing)) {
            for (int axis = 0; axis < 3; axis++) {
                result[axis] = Math.min(result[axis], part[axis]);
                result[axis + 3] = Math.max(result[axis + 3], part[axis + 3]);
            }
        }
        return result;
    }
}
