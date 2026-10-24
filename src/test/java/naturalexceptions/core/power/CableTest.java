package naturalexceptions.core.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Copper by the metre: its resistance, what it can carry, how it heats, and how it fails. */
final class CableTest {

    @Test
    void copperByGauge() {
        assertEquals(0.0168, Cable.LIGHT_BARE.ohmsPerMetre(), 1e-6, "1 mm2 of copper, a metre");
        assertTrue(Cable.HEAVY_BARE.ohmsPerMetre() < Cable.STANDARD_BARE.ohmsPerMetre());
        assertTrue(Cable.STANDARD_BARE.ohmsPerMetre() < Cable.LIGHT_BARE.ohmsPerMetre());
        assertEquals(
            Cable.LIGHT_BARE.ohmsPerMetre(),
            Cable.LIGHT_INSULATED.ohmsPerMetre(),
            1e-12,
            "cloth is not copper");
    }

    @Test
    void heavierCarriesMoreAndClothLimitsIt() {
        for (boolean insulated : new boolean[] { false, true }) {
            Cable light = insulated ? Cable.LIGHT_INSULATED : Cable.LIGHT_BARE;
            Cable standard = insulated ? Cable.STANDARD_INSULATED : Cable.STANDARD_BARE;
            Cable heavy = insulated ? Cable.HEAVY_INSULATED : Cable.HEAVY_BARE;
            assertTrue(light.ratedAmps() < standard.ratedAmps() && standard.ratedAmps() < heavy.ratedAmps());
        }
        assertTrue(
            Cable.LIGHT_INSULATED.ratedAmps() < Cable.LIGHT_BARE.ratedAmps(),
            "cloth chars long before copper melts");
        assertTrue(
            Cable.LIGHT_INSULATED.ratedAmps() > 10.0 && Cable.LIGHT_INSULATED.ratedAmps() < 40.0,
            "a light wire runs a lamp or two: " + Cable.LIGHT_INSULATED.ratedAmps());
        assertTrue(
            Cable.HEAVY_INSULATED.ratedAmps() > 80.0,
            "heavy cable for storm power: " + Cable.HEAVY_INSULATED.ratedAmps());
    }

    @Test
    void itWarmsTowardsWhereHeatInMeetsHeatOut() {
        Cable wire = Cable.LIGHT_BARE;
        double watts = 5.0, settled = Cable.AMBIENT_KELVIN + watts / wire.coolingPerKelvin();
        double t = Cable.AMBIENT_KELVIN;
        for (int i = 0; i < 10000; i++) t = wire.step(t, watts, 0.2); // a light wire settles over minutes
        assertEquals(settled, t, 0.5);
        double cooled = wire.step(settled, 0.0, 10000.0);
        assertEquals(Cable.AMBIENT_KELVIN, cooled, 0.01, "and cools back with no current");
        assertTrue(wire.step(Cable.AMBIENT_KELVIN, watts, 1.0) > Cable.AMBIENT_KELVIN);
    }

    @Test
    void clothCharsOffToBareCopperAndBareCopperMelts() {
        assertEquals(Cable.LIGHT_BARE, Cable.LIGHT_INSULATED.failed());
        assertEquals(Cable.STANDARD_BARE, Cable.STANDARD_INSULATED.failed());
        assertEquals(Cable.HEAVY_BARE, Cable.HEAVY_INSULATED.failed());
        assertNull(Cable.HEAVY_BARE.failed(), "melted copper is gone");
        assertEquals(Cable.CLOTH_LIMIT_KELVIN, Cable.STANDARD_INSULATED.limitKelvin());
        assertTrue(Cable.STANDARD_BARE.limitKelvin() > 1300.0, "copper melts near 1358 K");
    }
}
