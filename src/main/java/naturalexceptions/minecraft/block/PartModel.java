package naturalexceptions.minecraft.block;

import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A block drawn as a few axis-aligned boxes, each with its own small material texture (clay, metal, soot) rather
 * than one full-block picture cropped onto tiny geometry. The same boxes serve rendering, selection and collision.
 */
public interface PartModel {

    /** Boxes as {minX, minY, minZ, maxX, maxY, maxZ} in block space for this metadata. */
    double[][] parts(int metadata);

    /** Texture for the {@code index}th box. */
    @SideOnly(Side.CLIENT)
    IIcon partIcon(int index, int metadata);

    /** Metadata used to draw the block as an item. */
    int inventoryMetadata();

    /** Shared render type id, assigned when the client registers its renderer. */
    final class RenderType {

        public static int id = -1;

        private RenderType() {}
    }

    /** Rotates a box authored with the attachment wall on the -Z side to face {@code facing} (2..5). */
    static double[] rotate(double[] box, int facing) {
        double minX = box[0], minY = box[1], minZ = box[2], maxX = box[3], maxY = box[4], maxZ = box[5];
        switch (facing) {
            case 3:
                return new double[] { minX, minY, 1 - maxZ, maxX, maxY, 1 - minZ };
            case 4:
                return new double[] { minZ, minY, minX, maxZ, maxY, maxX };
            case 5:
                return new double[] { 1 - maxZ, minY, minX, 1 - minZ, maxY, maxX };
            default:
                return new double[] { minX, minY, minZ, maxX, maxY, maxZ };
        }
    }

    /** Smallest box containing all parts. */
    static double[] bounds(double[][] parts) {
        double[] result = { 1, 1, 1, 0, 0, 0 };
        for (double[] part : parts) {
            for (int axis = 0; axis < 3; axis++) {
                result[axis] = Math.min(result[axis], part[axis]);
                result[axis + 3] = Math.max(result[axis + 3], part[axis + 3]);
            }
        }
        return result;
    }
}
