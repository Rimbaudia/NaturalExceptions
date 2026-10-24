package naturalexceptions.core.worldgen;

/**
 * A ruined watch post of the people who built the sky-halls, on the tallest pillar of a cluster of spires: a round
 * stone floor a little wider than the pillar's tip, a low parapet, four posts holding up what is left of a roof, a
 * bed and a chest. A shaft with a ladder runs down the pillar's heart to a passage out at its foot, and a hatch opens
 * on the floor. Parts are given relative to the pillar's centre column and the floor's height.
 */
public final class HermitLookout {

    public static final double FLOOR_RADIUS = 3.2;
    /** Inside this the floor is whole and lived on; outside it, the parapet and the broken edge. */
    public static final double INNER_RADIUS = 2.3;
    /** The roof stands this far above the floor. */
    public static final int ROOF_HEIGHT = 4;
    /** Only a pillar this wide at its foot carries one: a narrower tip is too slight to build on. */
    public static final double MIN_PILLAR_RADIUS = 3.5;

    public enum Part {
        NONE,
        CLEAR,
        /** Under the floor, where the pillar's tip does not reach it. */
        FOOTING,
        FLOOR,
        FLOOR_MOSSY,
        FLOOR_CRACKED,
        PARAPET,
        POST,
        ROOF,
        HATCH,
        BED_FOOT,
        BED_HEAD,
        CHEST
    }

    private HermitLookout() {}

    /** The pillar that carries the lookout: the tallest wide enough to build on, or null if none is. */
    public static Landmarks.Pillar pillar(Landmarks.Landmark spires) {
        if (spires == null || spires.kind() != Landmarks.Kind.SPIRES) return null;
        Landmarks.Pillar best = null;
        for (Landmarks.Pillar pillar : spires.pillars())
            if (pillar.radius >= MIN_PILLAR_RADIUS && (best == null || pillar.topY > best.topY)) best = pillar;
        return best;
    }

    /** The floor's height on its pillar. */
    public static int floorY(Landmarks.Pillar pillar) {
        return pillar.topY + 1;
    }

    /** How far out from the pillar's centre the passage at its foot runs. */
    public static int passageLength(Landmarks.Pillar pillar) {
        return (int) Math.ceil(pillar.radius * 1.2) + 1;
    }

    /**
     * The part at (dx, dy, dz) from the floor's centre. {@code columnRoll} (0..1) is the same for the whole column,
     * so a broken floor has no parapet left above it; {@code blockRoll} varies block by block.
     */
    public static Part partAt(int dx, int dy, int dz, double columnRoll, double blockRoll) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > FLOOR_RADIUS) return Part.NONE;
        boolean edge = d > INNER_RADIUS;
        boolean post = Math.abs(dx) == 2 && Math.abs(dz) == 2;
        boolean broken = edge && !post && columnRoll < 0.15;
        if (dy == -1) return d <= INNER_RADIUS && d > 0 ? Part.FOOTING : Part.NONE; // the shaft runs up the middle
        if (dy == 0) {
            if (dx == 0 && dz == 0) return Part.HATCH;
            if (broken) return Part.NONE;
            return blockRoll < 0.25 ? Part.FLOOR_MOSSY : blockRoll < 0.4 ? Part.FLOOR_CRACKED : Part.FLOOR;
        }
        if (dy > 0 && dy < ROOF_HEIGHT) {
            if (post) return Part.POST;
            if (dy == 1) {
                if (edge) return broken || columnRoll < 0.35 ? Part.CLEAR : Part.PARAPET;
                if (dx == 1 && dz == 1) return Part.BED_FOOT;
                if (dx == 1 && dz == 2) return Part.BED_HEAD;
                if (dx == -1 && dz == 1) return Part.CHEST;
            }
            return Part.CLEAR;
        }
        if (dy == ROOF_HEIGHT) return post || blockRoll >= 0.35 && !broken ? Part.ROOF : Part.CLEAR;
        return Part.NONE;
    }
}
