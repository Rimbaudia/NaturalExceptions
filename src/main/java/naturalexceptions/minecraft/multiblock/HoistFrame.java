package naturalexceptions.minecraft.multiblock;

/**
 * The frame a windlass works, as found in the world: where its rope runs over its sheave, the ground it stands on,
 * and for a derrick where its mast and boom are. Made by {@link HeadframeDefinition} and {@link DerrickDefinition}.
 */
public final class HoistFrame {

    public enum Kind {
        HEADFRAME,
        DERRICK
    }

    public final Kind kind;
    public final int sheaveX, sheaveY, sheaveZ, groundY;
    /** A derrick's pivot, the height of its mast's top, its boom's direction (vanilla side, 2..5) and length. */
    public final int pivotX, pivotY, pivotZ, mastTop, boomFacing, boomLength;

    HoistFrame(Kind kind, int sheaveX, int sheaveY, int sheaveZ, int groundY, int pivotX, int pivotY, int pivotZ,
        int mastTop, int boomFacing, int boomLength) {
        this.kind = kind;
        this.sheaveX = sheaveX;
        this.sheaveY = sheaveY;
        this.sheaveZ = sheaveZ;
        this.groundY = groundY;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;
        this.mastTop = mastTop;
        this.boomFacing = boomFacing;
        this.boomLength = boomLength;
    }

    static HoistFrame headframe(int sheaveX, int sheaveY, int sheaveZ, int groundY) {
        return new HoistFrame(Kind.HEADFRAME, sheaveX, sheaveY, sheaveZ, groundY, 0, 0, 0, 0, 0, 0);
    }

    public boolean sameAs(HoistFrame o) {
        return o != null && o.kind == kind && o.sheaveX == sheaveX && o.sheaveY == sheaveY && o.sheaveZ == sheaveZ;
    }

    public int height() {
        return sheaveY - groundY - 1;
    }
}
