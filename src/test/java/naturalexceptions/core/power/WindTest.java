package naturalexceptions.core.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** No wind on the ground, full wind up in the sky, more in a storm, and a windmill gives more in more wind. */
final class WindTest {

    @Test
    void itRisesWithHeightAndStorms() {
        assertEquals(0.0, Wind.at(0, 60, 0, 1000, false, false), 1e-9, "calm on the ground");
        double mid = Wind.at(0, 95, 0, 1000, false, false), sky = Wind.at(0, 160, 0, 1000, false, false);
        assertTrue(mid > 0.0 && mid < sky, mid + " < " + sky);
        assertTrue(sky >= 0.6 && sky <= 1.0, "full wind gusts round 0.6 to 1: " + sky);
        assertTrue(Wind.at(0, 160, 0, 1000, true, true) > Wind.at(0, 160, 0, 1000, true, false));
        assertTrue(Wind.at(0, 160, 0, 1000, true, false) > sky);
    }

    @Test
    void itGustsButIsTheSameForEveryoneAtOnce() {
        double a = Wind.at(10, 150, 20, 5000, false, false), b = Wind.at(10, 150, 20, 5000, false, false);
        assertEquals(a, b, 0.0, "the client and the server agree");
        double lo = 9, hi = 0;
        for (long t = 0; t < 200000; t += 997) {
            double w = Wind.at(10, 150, 20, t, false, false);
            lo = Math.min(lo, w);
            hi = Math.max(hi, w);
        }
        assertTrue(hi - lo > 0.2, "it gusts: " + lo + ".." + hi);
    }

    @Test
    void moreWindGivesMoreCurrent() {
        assertTrue(Wind.internalOhms(1.0) < Wind.internalOhms(0.3));
        double full = 24.0 / Wind.internalOhms(1.0);
        assertTrue(full > 30.0, "in full wind it can run several lamps: " + full + " A short-circuit");
    }
}
