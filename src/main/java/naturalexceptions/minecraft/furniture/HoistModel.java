package naturalexceptions.minecraft.furniture;

/** What a hoist's renderers need from the furniture package: the basket's shape, and where a ride is. */
public final class HoistModel {

    private HoistModel() {}

    public static double[][] basket() {
        return BlockHoist.BASKET;
    }

    public static double[] feet(EntityHoistHook hook, float partialTicks) {
        return hook.feetBetweenTicks(partialTicks);
    }

    public static double ropeTop(EntityHoistHook hook) {
        return hook.ropeTop;
    }
}
