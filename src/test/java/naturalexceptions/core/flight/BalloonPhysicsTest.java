package naturalexceptions.core.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A balloon carries a rider up to the islands on its burner, sinks on its pilot, and goes where the wind goes. */
final class BalloonPhysicsTest {

    static final double RIDER = 80.0;

    @Test
    void itCanCarryARiderToTheIslandsWithinWhatItsClothTakes() {
        double ground = BalloonPhysics.hoverKelvin(RIDER, 64), sky = BalloonPhysics.hoverKelvin(RIDER, 190);
        assertTrue(ground > BalloonPhysics.AMBIENT_K + 30, "it needs real heat to float: " + ground);
        assertTrue(sky > ground, "thinner air up high wants it hotter");
        assertTrue(sky < BalloonPhysics.ENVELOPE_LIMIT_K - 20, "but it still floats at the islands' height: " + sky);
        assertEquals(BalloonPhysics.AMBIENT_K, BalloonPhysics.warm(250.0, 0.0, false, 1.0), 1e-9, "never colder than the air");
    }

    @Test
    void theBurnerTakesItUpThePilotLetsItDown() {
        double kelvin = BalloonPhysics.AMBIENT_K, speed = 0.0, y = 64.0;
        int seconds = 0;
        while (y < 190.0 && seconds < 300) {
            for (int t = 0; t < 20; t++) {
                kelvin = BalloonPhysics.warm(kelvin, BalloonPhysics.BURNER_W, false, 0.05);
                speed = BalloonPhysics.climb(speed, kelvin, RIDER, y, 0.05);
                if (y <= 64.0 && speed < 0) speed = 0;
                y += speed * 0.05;
            }
            seconds++;
        }
        assertTrue(y >= 190.0, "the burner gets it up to the islands");
        assertTrue(seconds < 120, "in a couple of minutes at most: " + seconds + " s");
        assertTrue(speed < 10.0, "and it rises gently, held by the air: " + speed + " m/s");
        // On the pilot alone it cools, and comes down.
        for (int t = 0; t < 20 * 60; t++) {
            kelvin = BalloonPhysics.warm(kelvin, BalloonPhysics.PILOT_W, false, 0.05);
            speed = BalloonPhysics.climb(speed, kelvin, RIDER, y, 0.05);
        }
        assertTrue(speed < 0.0, "on its pilot it sinks");
        assertTrue(speed > -12.0, "but not like a stone: " + speed);
        double vented = BalloonPhysics.warm(380.0, 0.0, true, 1.0), shut = BalloonPhysics.warm(380.0, 0.0, false, 1.0);
        assertTrue(vented < shut, "the vent dumps heat");
    }

    @Test
    void theWindTurnsWithHeightSoAPilotSteersByClimbing() {
        double low = BalloonPhysics.windBearing(64, 1000), high = BalloonPhysics.windBearing(164, 1000);
        assertTrue(Math.abs(high - low) > Math.PI / 4, "a hundred blocks higher, the wind blows another way");
        double[] v = { 0.0, 0.0 };
        for (int t = 0; t < 600; t++) v = BalloonPhysics.drift(v[0], v[1], 1.0, 0.0, 0.05);
        assertEquals(BalloonPhysics.WIND_MPS, v[0], 0.2, "it comes round to the wind's speed");
        assertEquals(0.0, v[1], 1e-9);
    }
}
