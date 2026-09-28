package naturalexceptions.core.worldgen;

import java.util.Objects;

import naturalexceptions.core.persistence.WorldgenEpoch;

/**
 * Stateless, cache-independent coarse Overworld truth. Minecraft coordinates are inputs, not persistent identities;
 * every field is keyed by the world seed, semantic channel, and saved generation epoch.
 */
public final class OverworldGeneration {

    private static final long ELEVATION_SALT = 0x2d358dccaa6c78a5L;
    private static final long MOISTURE_SALT = 0x4b1d5a9f7c3e2b61L;
    private static final long TEMPERATURE_SALT = 0x7f4a7c159e3779b9L;
    private static final long GEOLOGY_SALT = 0x6a09e667f3bcc909L;

    private final long worldSeed;
    private final WorldgenEpoch epoch;
    private final long epochSalt;

    public OverworldGeneration(long worldSeed, WorldgenEpoch epoch) {
        this.worldSeed = worldSeed;
        this.epoch = Objects.requireNonNull(epoch, "epoch");
        this.epochSalt = mix64(((long) epoch.value() << 32) ^ 0x4e45576f726c6467L);
    }

    public long worldSeed() {
        return worldSeed;
    }

    public WorldgenEpoch epoch() {
        return epoch;
    }

    /** Stable coordinate-keyed choice used for sparse ecology and materialization decisions. */
    public boolean passesChanceAt(int x, int z, long channel, double probability) {
        if (probability < 0.0 || probability > 1.0 || Double.isNaN(probability)) {
            throw new IllegalArgumentException("probability must be in [0, 1]");
        }
        if (probability == 0.0) return false;
        if (probability == 1.0) return true;
        long value = worldSeed ^ epochSalt ^ channel;
        value ^= mix64((long) x * 0x9e3779b97f4a7c15L);
        value ^= mix64((long) z * 0xc2b2ae3d27d4eb4fL);
        double unit = (mix64(value) >>> 11) * 0x1.0p-53;
        return unit < probability;
    }

    public OverworldSample sample(int x, int z) {
        double elevation = clamp01(
            0.5 + 0.34 * valueField(x, z, 640, ELEVATION_SALT)
                + 0.16 * valueField(x, z, 176, ELEVATION_SALT ^ 0x1337L));
        double moisture = clamp01(
            0.5 + 0.40 * valueField(x, z, 416, MOISTURE_SALT) - Math.max(0.0, elevation - 0.68) * 0.20);
        double temperature = clamp01(0.5 + 0.40 * valueField(x, z, 768, TEMPERATURE_SALT));
        double geologyValue = valueField(x, z, 544, GEOLOGY_SALT);
        GeologicalProvince geology = classifyGeology(geologyValue);

        double west = rawElevation(x - 32, z);
        double east = rawElevation(x + 32, z);
        double north = rawElevation(x, z - 32);
        double south = rawElevation(x, z + 32);
        double slope = clamp01((Math.abs(east - west) + Math.abs(south - north)) * 0.5);

        HydrologicalContext hydrology = classifyHydrology(elevation, moisture, slope);
        SoilContext soil = classifySoil(elevation, moisture, slope, geology, hydrology);
        double ecology = ecologySuitability(temperature, moisture, slope, soil);
        return new OverworldSample(elevation, moisture, temperature, slope, geology, hydrology, soil, ecology);
    }

    private GeologicalProvince classifyGeology(double value) {
        if (value > 0.28) return GeologicalProvince.BASALTIC;
        if (value < -0.28) return GeologicalProvince.SEDIMENTARY;
        return GeologicalProvince.GRANITIC;
    }

    private HydrologicalContext classifyHydrology(double elevation, double moisture, double slope) {
        if (moisture > 0.69 && elevation < 0.43) return HydrologicalContext.WET_BASIN;
        if (slope > 0.19) return HydrologicalContext.DRAINAGE_SLOPE;
        if (moisture < 0.34) return HydrologicalContext.DRY_UPLAND;
        return HydrologicalContext.MOIST_PLAIN;
    }

    private SoilContext classifySoil(double elevation, double moisture, double slope, GeologicalProvince geology,
        HydrologicalContext hydrology) {
        if (hydrology == HydrologicalContext.WET_BASIN) return SoilContext.ALLUVIAL;
        if (moisture < 0.27 && elevation > 0.48) return SoilContext.DRY_SAND;
        if (slope > 0.24 || geology == GeologicalProvince.BASALTIC && elevation > 0.78) {
            return SoilContext.THIN_ROCKY;
        }
        return SoilContext.LOAM;
    }

    private double ecologySuitability(double temperature, double moisture, double slope, SoilContext soil) {
        double thermal = 1.0 - Math.abs(temperature - 0.52) * 1.65;
        double water = 1.0 - Math.abs(moisture - 0.58) * 1.55;
        double fertility = soil == SoilContext.LOAM || soil == SoilContext.ALLUVIAL ? 1.0 : 0.58;
        return clamp01((thermal * 0.38 + water * 0.42 + fertility * 0.20) * (1.0 - Math.min(0.7, slope)));
    }

    private double rawElevation(int x, int z) {
        return clamp01(
            0.5 + 0.34 * valueField(x, z, 640, ELEVATION_SALT)
                + 0.16 * valueField(x, z, 176, ELEVATION_SALT ^ 0x1337L));
    }

    private double valueField(int x, int z, int scale, long channelSalt) {
        long cellX = Math.floorDiv(x, scale);
        long cellZ = Math.floorDiv(z, scale);
        double localX = Math.floorMod(x, scale) / (double) scale;
        double localZ = Math.floorMod(z, scale) / (double) scale;
        localX = smooth(localX);
        localZ = smooth(localZ);
        double a = lattice(cellX, cellZ, channelSalt);
        double b = lattice(cellX + 1, cellZ, channelSalt);
        double c = lattice(cellX, cellZ + 1, channelSalt);
        double d = lattice(cellX + 1, cellZ + 1, channelSalt);
        return lerp(lerp(a, b, localX), lerp(c, d, localX), localZ);
    }

    private double lattice(long x, long z, long channelSalt) {
        long value = mix64(
            worldSeed ^ epochSalt ^ channelSalt ^ mix64(x * 0x9e3779b97f4a7c15L) ^ mix64(z * 0xc2b2ae3d27d4eb4fL));
        return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static double smooth(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
