package naturalexceptions.core.worldgen;

/**
 * Deterministic hashing and smooth value noise for landmark shapes. Pure functions of the seed and world
 * coordinates, so any chunk evaluating a landmark sees exactly the same shape.
 */
public final class LandmarkNoise {

    private final long seed;

    public LandmarkNoise(long seed) {
        this.seed = seed;
    }

    public long seed() {
        return seed;
    }

    /** A uniform value in [0, 1) for integer inputs on a named lane. */
    public double unit(long a, long b, long lane) {
        long value = seed ^ 0x4c414e444d41524bL;
        value ^= mix64(a * 0x9e3779b97f4a7c15L);
        value ^= mix64(b * 0xc2b2ae3d27d4eb4fL);
        value ^= mix64(lane * 0x165667b19e3779f9L);
        return (mix64(value) >>> 11) * 0x1.0p-53;
    }

    /** A uniform value in [0, 1) for a block position on a named lane. */
    public double unit3(int x, int y, int z, long lane) {
        return unit(x * 31L + y * 0x1f1f1fL, z, lane);
    }

    /** Smooth 2D value noise in [0, 1) with the given lattice spacing in blocks. */
    public double smooth(double x, double z, double spacing, long lane) {
        double fx = x / spacing;
        double fz = z / spacing;
        long x0 = (long) Math.floor(fx);
        long z0 = (long) Math.floor(fz);
        double tx = fade(fx - x0);
        double tz = fade(fz - z0);
        double a = unit(x0, z0, lane);
        double b = unit(x0 + 1, z0, lane);
        double c = unit(x0, z0 + 1, lane);
        double d = unit(x0 + 1, z0 + 1, lane);
        double top = a + (b - a) * tx;
        double bottom = c + (d - c) * tx;
        return top + (bottom - top) * tz;
    }

    /** Smooth noise around a circle, in [0, 1): for ragged outlines that still close on themselves. */
    public double around(double angle, int lobes, long lane) {
        double t = (angle / (2.0 * Math.PI) + 1.0) % 1.0 * lobes;
        int i0 = (int) Math.floor(t) % lobes;
        int i1 = (i0 + 1) % lobes;
        double f = fade(t - Math.floor(t));
        double a = unit(i0, lobes, lane);
        double b = unit(i1, lobes, lane);
        return a + (b - a) * f;
    }

    private static double fade(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
