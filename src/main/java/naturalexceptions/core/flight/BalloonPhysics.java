package naturalexceptions.core.flight;

/**
 * A hot-air balloon's physics, per tick. The envelope holds {@link #ENVELOPE_M3} of air; warmed above the air outside
 * it is lighter, and the difference lifts the balloon (Archimedes: the weight of the air pushed aside, less the weight
 * of the warm air in it). A brazier under the mouth warms it; the envelope loses heat to the air, and a vent dumps it
 * fast. Rising and falling meet the air's drag. Sideways it goes with the wind, which blows a different way at each
 * height, so a pilot steers by climbing and sinking.
 *
 * <p>
 * Units are SI (metres, kilograms, seconds, kelvin, watts); a block is a metre.
 */
public final class BalloonPhysics {

    private BalloonPhysics() {}

    public static final double G = 9.81;
    /** Air at the ground, and how it thins with height (its scale height, in blocks, shortened to the sky's span). */
    public static final double AIR_DENSITY = 1.2, SEA_LEVEL_Y = 63.0, THIN_OVER = 1500.0;
    public static final double AMBIENT_K = 293.15;
    /** The envelope: its volume, and the hottest its cloth takes before it scorches (the burner holds back there). */
    public static final double ENVELOPE_M3 = 900.0, ENVELOPE_LIMIT_K = 400.0;
    /** What it weighs, empty: envelope, basket, brazier, rope. */
    public static final double EMPTY_KG = 60.0 + 30.0 + 15.0 + 5.0;
    /**
     * How much heat it takes to warm the envelope's air a kelvin. The air in it turns over through its open mouth, so
     * it warms and cools far faster than a sealed bag would.
     */
    public static final double HEAT_PER_K = 15000.0;
    /** Heat lost through the cloth per kelvin over the air outside, and with the vent pulled. */
    public static final double LOSS_W_PER_K = 300.0, VENT_W_PER_K = 2400.0;
    /** The brazier: opened up, and burning low (its pilot). */
    public static final double BURNER_W = 45000.0, PILOT_W = 9000.0;
    /** Drag as it rises or sinks: its coefficient and the envelope's cross-section. */
    public static final double DRAG_COEFF = 0.5, AREA_M2 = 70.0;
    /** How fast it comes round to the wind (per second), and the wind's speed at full strength. */
    public static final double WIND_GRIP = 0.25, WIND_MPS = 6.0;
    /** The wind turns this much (radians) for each block higher, and slowly with the day. */
    public static final double TURN_PER_BLOCK = 0.012, TURN_PER_TICK = 0.00004;

    /** The air's density at height y. */
    public static double airDensity(double y) {
        return AIR_DENSITY * Math.exp(-(y - SEA_LEVEL_Y) / THIN_OVER);
    }

    /** The lift (newtons) of the envelope's air at temperature {@code kelvin}, at height y. */
    public static double lift(double kelvin, double y) {
        return airDensity(y) * ENVELOPE_M3 * G * (1.0 - AMBIENT_K / Math.max(AMBIENT_K, kelvin));
    }

    /** The temperature at which the balloon, carrying {@code loadKg}, just floats at height y (may be past the limit). */
    public static double hoverKelvin(double loadKg, double y) {
        double share = (EMPTY_KG + loadKg) / (airDensity(y) * ENVELOPE_M3);
        return share >= 1.0 ? Double.POSITIVE_INFINITY : AMBIENT_K / (1.0 - share);
    }

    /** The envelope's temperature after {@code seconds} with {@code burnerWatts} going in and the vent open or shut. */
    public static double warm(double kelvin, double burnerWatts, boolean vent, double seconds) {
        double over = kelvin - AMBIENT_K;
        double loss = (LOSS_W_PER_K + (vent ? VENT_W_PER_K : 0.0)) * over;
        double next = kelvin + (burnerWatts - loss) * seconds / HEAT_PER_K;
        return Math.max(AMBIENT_K, Math.min(ENVELOPE_LIMIT_K, next));
    }

    /**
     * The new vertical speed (m/s, up positive) after {@code seconds}: lift less weight, less drag, over everything
     * that has to move (the balloon, its load, and the air in its envelope).
     */
    public static double climb(double speed, double kelvin, double loadKg, double y, double seconds) {
        double rho = airDensity(y);
        double mass = EMPTY_KG + loadKg + rho * ENVELOPE_M3 * AMBIENT_K / Math.max(AMBIENT_K, kelvin);
        double force = lift(kelvin, y) - (EMPTY_KG + loadKg) * G;
        double drag = 0.5 * rho * DRAG_COEFF * AREA_M2 * speed * Math.abs(speed);
        return speed + (force - drag) / mass * seconds;
    }

    /** The bearing (radians) the wind blows toward at height y and world time {@code ticks}. */
    public static double windBearing(double y, long ticks) {
        return ticks * TURN_PER_TICK + y * TURN_PER_BLOCK;
    }

    /**
     * The new sideways velocity {vx, vz} (m/s) after {@code seconds}, coming round to a wind of {@code strength} (0 to
     * about 1.5) blowing toward {@code bearing}.
     */
    public static double[] drift(double vx, double vz, double strength, double bearing, double seconds) {
        double wx = Math.cos(bearing) * strength * WIND_MPS, wz = Math.sin(bearing) * strength * WIND_MPS;
        double k = Math.min(1.0, WIND_GRIP * seconds);
        return new double[] { vx + (wx - vx) * k, vz + (wz - vz) * k };
    }
}
