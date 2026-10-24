package naturalexceptions.core.power;

/**
 * How hard the wind blows at a place and time: nothing on the ground, rising with height to full above
 * {@link #FULL_Y} (the sky islands stand in it), stronger in rain and a storm, and gusting. The same on the server and
 * the client, from the time and the place alone, so a windmill's sails turn as fast as its generator is driven.
 */
public final class Wind {

    /** No wind to speak of below the first height, full wind above the second. */
    public static final int CALM_Y = 62, FULL_Y = 128;
    public static final double RAIN = 1.25, STORM = 1.5;
    /** Below this a windmill does not turn. */
    public static final double CUT_IN = 0.15;

    private Wind() {}

    /** The wind's strength (0 to about 1.5). */
    public static double at(int x, int y, int z, long time, boolean raining, boolean thundering) {
        double height = Math.max(0.0, Math.min(1.0, (y - CALM_Y) / (double) (FULL_Y - CALM_Y)));
        double gust = 0.8 + 0.2 * Math.sin(time * 0.0021 + x * 0.013) * Math.sin(time * 0.00073 + z * 0.017);
        double weather = thundering ? STORM : raining ? RAIN : 1.0;
        return height * gust * weather;
    }

    /** A windmill's generator: its governor holds the voltage; the wind decides how much current it can give. */
    public static double internalOhms(double wind) {
        return 0.6 / Math.max(wind, 1.0e-3);
    }
}
