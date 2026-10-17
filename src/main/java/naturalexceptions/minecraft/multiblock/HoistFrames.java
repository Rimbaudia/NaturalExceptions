package naturalexceptions.minecraft.multiblock;

import net.minecraft.world.IBlockAccess;

/** The frame a windlass works: a headframe, or else a derrick. */
public final class HoistFrames {

    private HoistFrames() {}

    public static HoistFrame find(IBlockAccess world, int x, int y, int z) {
        HoistFrame frame = HeadframeDefinition.INSTANCE.find(world, x, y, z);
        return frame != null ? frame : DerrickDefinition.INSTANCE.find(world, x, y, z);
    }

    /** What a windlass with no frame to work is missing, for its user. */
    public static String missing(IBlockAccess world, int x, int y, int z) {
        return "This windlass has nothing to work. A headframe: "
            + HeadframeDefinition.INSTANCE.validate(world, x, y, z, 2)
                .failureReason()
            + ". Or a derrick: "
            + DerrickDefinition.INSTANCE.validate(world, x, y, z, 2)
                .failureReason()
            + ".";
    }
}
